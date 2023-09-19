/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.zebrunner.carina.utils.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.ZipManager;
import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.config.Configuration;

/*
 * Be careful with LOGGER usage here because potentially it could do recursive call together with ThreadLogAppender functionality
 */
public class ReportContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ROOT_DIR_SYSTEM_PROPERTY = "user.dir";
    private static final String FOLDERS_FORMAT = "%s/%s";
    private static final String SPACE_PATTERN = "[^a-zA-Z0-9.-]";
    private static final String GALLERY_ZIP = "gallery-lib.zip";
    public static final String TEMP_FOLDER = "temp";
    private static File baseDirectory = null;
    private static File tempDirectory;
    private static long rootID;
    private static final ThreadLocal<File> testDirectory = new InheritableThreadLocal<>();
    private static final ThreadLocal<Boolean> isCustomTestDirName = new InheritableThreadLocal<>();

    private ReportContext() {
    }

    /**
     * Creates base directory for tests execution to save screenshots, logs etc
     * 
     * @return base root folder for run.
     */
    public static File getBaseDir() {
        if (baseDirectory == null) {
            File projectRoot = new File(String.format(FOLDERS_FORMAT, URLDecoder.decode(System.getProperty(ROOT_DIR_SYSTEM_PROPERTY),
                    StandardCharsets.UTF_8),
                    Configuration.getRequired(Configuration.Parameter.PROJECT_REPORT_DIRECTORY)));
            if (!projectRoot.exists()) {
                boolean isCreated = projectRoot.mkdirs();
                if (!isCreated) {
                    throw new RuntimeException("Folder not created: " + projectRoot.getAbsolutePath());
                }
            }
            rootID = System.currentTimeMillis();
            String directory = String.format("%s/%s/%d", URLDecoder.decode(System.getProperty(ROOT_DIR_SYSTEM_PROPERTY), StandardCharsets.UTF_8),
                    Configuration.getRequired(Configuration.Parameter.PROJECT_REPORT_DIRECTORY), rootID);
            File baseDirectoryTmp = new File(directory);
            boolean isCreated = baseDirectoryTmp.mkdir();
            if (!isCreated) {
                throw new RuntimeException("Folder not created: " + directory);
            }

            baseDirectory = baseDirectoryTmp;

            copyGalleryLib();
        }
        return baseDirectory;
    }

    /**
     * Creates temp directory for tests execution
     * 
     * @return temp folder for run.
     */
    public static synchronized File getTempDir() {
        if (tempDirectory == null) {
            tempDirectory = new File(String.format(FOLDERS_FORMAT, getBaseDir().getAbsolutePath(), TEMP_FOLDER));
            boolean isCreated = tempDirectory.mkdir();
            if (!isCreated) {
                throw new RuntimeException("Folder not created: " + tempDirectory.getAbsolutePath());
            }
        }
        return tempDirectory;
    }

    /**
     * Creates unique test directory for test
     * 
     * @return test log/screenshot folder.
     */
    public static File getTestDir() {
        return getTestDir(StringUtils.EMPTY);
    }

    /**
     * Creates unique test directory for test
     * 
     * @param dirName String
     * @return test log/screenshot folder.
     */
    private static File getTestDir(String dirName) {
        File testDir = testDirectory.get();
        if (testDir == null) {
            testDir = createTestDir(dirName);
        }
        return testDir;
    }

    /**
     * Rename test directory to custom name.
     * 
     * @param dirName String
     * @return test report dir
     */
    public static synchronized File setCustomTestDirName(String dirName) {
        isCustomTestDirName.set(Boolean.FALSE);
        File testDir = testDirectory.get();
        if (testDir == null) {
            LOGGER.debug("Test dir will be created.");
            testDir = getTestDir(dirName);
        } else {
            LOGGER.debug("Test dir will be renamed to custom name.");
            renameTestDir(dirName);
        }
        isCustomTestDirName.set(Boolean.TRUE);
        return testDir;
    }

    public static void emptyTestDirData() {
        testDirectory.remove();
        isCustomTestDirName.set(Boolean.FALSE);
        stopThreadLogAppender();
    }

    public static synchronized File createTestDir() {
        return createTestDir(UUID.randomUUID().toString());
    }

    private static synchronized File createTestDir(String dirName) {
        File testDir;
        String directory = String.format(FOLDERS_FORMAT, getBaseDir(), dirName);

        testDir = new File(directory);
        if (!testDir.exists()) {
            testDir.mkdirs();
            if (!testDir.exists()) {
                throw new RuntimeException("Test Folder(s) not created: " + testDir.getAbsolutePath());
            }
        }

        testDirectory.set(testDir);
        return testDir;
    }

    private static void stopThreadLogAppender() {
        try {
            Class<?> logManagerClass = ClassUtils.getClass("org.apache.logging.log4j.LogManager");
            Object loggerContext = MethodUtils.invokeStaticMethod(logManagerClass, "getContext", true);
            Object configuration = MethodUtils.invokeMethod(loggerContext, "getConfiguration");
            Object appender = MethodUtils.invokeMethod(configuration, "getAppender", "ThreadLogAppender");
            if (appender != null) {
                MethodUtils.invokeMethod(appender, "stop");
            }
        } catch (Exception e) {
            LOGGER.debug("Exception while closing thread log appender.", e);
        }
    }

    private static File renameTestDir(String test) {
        File testDir = testDirectory.get();
        initIsCustomTestDir();
        if (testDir != null && !isCustomTestDirName.get()) {
            File newTestDir = new File(String.format(FOLDERS_FORMAT, getBaseDir(), test.replaceAll(SPACE_PATTERN, "_")));

            if (!newTestDir.exists()) {
                boolean isRenamed = false;
                int retry = 5;
                while (!isRenamed && retry > 0) {
                    // close ThreadLogAppender resources before renaming
                    stopThreadLogAppender();
                    isRenamed = testDir.renameTo(newTestDir);
                    if (!isRenamed) {
                        CommonUtils.pause(1);
                        System.err.println("renaming failed to '" + newTestDir + "'");
                    }
                    retry--;
                }

                if (isRenamed) {
                    testDirectory.set(newTestDir);
                    System.out.println("Test directory renamed to '" + newTestDir + "'");
                }
            }
        } else {
            LOGGER.error("Unexpected case with absence of test.log for '{}'", test);
        }

        return testDir;
    }

    private static void initIsCustomTestDir() {
        if (isCustomTestDirName.get() == null) {
            isCustomTestDirName.set(Boolean.FALSE);
        }
    }

    private static void copyGalleryLib() {
        String filesSeparator = FileSystems.getDefault().getSeparator();
        File reportsRootDir = new File(
                System.getProperty(ROOT_DIR_SYSTEM_PROPERTY) + "/" + Configuration.getRequired(Configuration.Parameter.PROJECT_REPORT_DIRECTORY));
        if (!new File(reportsRootDir.getAbsolutePath() + "/gallery-lib").exists()) {
            try {
                InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(GALLERY_ZIP);
                if (is == null) {
                    System.out.println("Unable to find in classpath: " + GALLERY_ZIP);
                    return;
                }
                ZipManager.copyInputStream(is, new BufferedOutputStream(new FileOutputStream(reportsRootDir.getAbsolutePath() + "/"
                        + GALLERY_ZIP)));
                ZipManager.unzip(reportsRootDir.getAbsolutePath() + filesSeparator + GALLERY_ZIP, reportsRootDir.getAbsolutePath());
                File zip = new File(reportsRootDir.getAbsolutePath() + filesSeparator + GALLERY_ZIP);
                boolean isSuccessful = zip.delete();
                if (!isSuccessful) {
                    System.out.println("Unable to delete zip: " + zip.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("Unable to copyGalleryLib! " + e);
            }
        }
    }

    public static class CustomAuthenticator extends Authenticator {

        String username;
        String password;

        public CustomAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

}
