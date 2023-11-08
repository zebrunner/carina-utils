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
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.ZipManager;
import com.zebrunner.carina.utils.config.Configuration;

/**
 * Offers methods for working with test folders.<br>
 * Important: <b>Be careful with LOGGER here because potentially it could do recursive call together with ThreadLogAppender functionality</b>
 */
public class ReportContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String GALLERY_ZIP = "gallery-lib.zip";
    private static final ThreadLocal<LazyInitializer<Path>> TEST_DIRECTORY = new InheritableThreadLocal<>();

    private static final LazyInitializer<Path> PROJECT_REPORT_DIRECTORY_INITIALIZER = new LazyInitializer<>() {
        @Override
        protected Path initialize() throws ConcurrentException {
            try {
                // "user.dir" - root dir system property
                Path projectReportDirectory = Path.of(URLDecoder.decode(System.getProperty("user.dir"), StandardCharsets.UTF_8))
                        .resolve(Configuration.getRequired(Configuration.Parameter.PROJECT_REPORT_DIRECTORY))
                        .normalize();

                if (!Files.isDirectory(projectReportDirectory)) {
                    Files.createDirectories(projectReportDirectory);
                }
                return projectReportDirectory;
            } catch (IOException e) {
                return ExceptionUtils.rethrow(e);
            }
        }
    };

    private static final LazyInitializer<Path> BASE_DIRECTORY_INITIALIZER = new LazyInitializer<>() {
        @Override
        protected Path initialize() throws ConcurrentException {
            try {
                Path baseDirectory = Files.createDirectories(getProjectReportFolder()
                        .resolve(String.valueOf(System.currentTimeMillis())));
                copyGalleryLib();
                return baseDirectory;
            } catch (IOException e) {
                return ExceptionUtils.rethrow(e);
            }
        }
    };

    private static final LazyInitializer<Path> TEMP_DIRECTORY_INITIALIZER = new LazyInitializer<>() {
        @Override
        protected Path initialize() throws ConcurrentException {
            try {
                Path path = BASE_DIRECTORY_INITIALIZER.get()
                        .resolve("temp");
                return Files.createDirectories(path);
            } catch (IOException e) {
                return ExceptionUtils.rethrow(e);
            }
        }
    };

    private ReportContext() {
        // hide
    }

    /**
     * Get directory of current test run.
     * If the folder does not exist, it will be created. <br>
     * Example folder name: {@code 1695289407136}
     * 
     * @return {@link Path}
     */
    public static Path getBaseDirectory() {
        try {
            return BASE_DIRECTORY_INITIALIZER.get();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Get temporary directory. This directory is located
     * (will be located if not already created) in the base directory with the {@code temp} name
     * 
     * @return {@link Path}
     */
    public static Path getTempDirectory() {
        try {
            return TEMP_DIRECTORY_INITIALIZER.get();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Get test directory.
     * If the folder does not exist, it will be created. <br>
     * Example folder name: {@code 4e2abbca-6512-460a-a280-feeb3dd8b0ee}. If folder renamed using {@link #renameTestDirectory(String)},
     * it will have another name.
     * <br>
     * For the cases when this method called before test folder initialized, base directory will be used.
     * 
     * @return {@link Path}
     */
    public static Path getTestDirectory() {
        if (TEST_DIRECTORY.get() == null) {
            return getBaseDirectory();
        }

        try {
            return TEST_DIRECTORY.get()
                    .get();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Initialize test directory
     * <b>For internal usage only</b>
     * 
     * @return {@link Path}
     */
    public static Path initTestDirectory() {
        if (TEST_DIRECTORY.get() == null) {
            TEST_DIRECTORY.set(new LazyInitializer<Path>() {
                @Override
                protected Path initialize() throws ConcurrentException {
                    try {
                        return Files.createDirectories(getBaseDirectory().resolve(UUID.randomUUID().toString()));
                    } catch (IOException e) {
                        return ExceptionUtils.rethrow(e);
                    }
                }
            });
        }
        try {
            return TEST_DIRECTORY.get().get();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Rename test directory.
     * 
     * @param name custom name of the test directory
     * @return {@link Path}
     */
    public static Path renameTestDirectory(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name must not be null, empty or contains whitespaces only.");
        }

        if (TEST_DIRECTORY.get() == null) {
            throw new IllegalStateException("Cannot rename test directory. Method called before test directory initialized.");
        }

        // replace spaces by _
        TEST_DIRECTORY.set(new RenameTestFolderInitializer<>(getTestDirectory(), RegExUtils.replaceAll(name, "[^a-zA-Z0-9.-]", "_")) {
            @Override
            protected Path initialize() throws ConcurrentException {
                try {
                    // close ThreadLogAppender resources before renaming
                    stopThreadLogAppender();
                    return Files.move(getPreviousTestDirectoryPath(), getBaseDirectory().resolve(getNewDirectoryName()));
                } catch (IOException e) {
                    return ExceptionUtils.rethrow(e);
                }
            }
        });
        try {
            return TEST_DIRECTORY.get()
                    .get();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * <b>For internal usage only</b>
     */
    @SuppressWarnings("unused")
    public static void emptyTestDirData() {
        TEST_DIRECTORY.remove();
        stopThreadLogAppender();
    }

    private static Path getProjectReportFolder() {
        try {
            return PROJECT_REPORT_DIRECTORY_INITIALIZER.get();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
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

    private abstract static class RenameTestFolderInitializer<T> extends LazyInitializer<T> {
        private final Path previousTestDirectoryPath;
        private final String newDirectoryName;

        public RenameTestFolderInitializer(Path previousTestDirectoryPath, String newDirectoryName) {
            super();
            this.previousTestDirectoryPath = previousTestDirectoryPath;
            this.newDirectoryName = newDirectoryName;
        }

        public Path getPreviousTestDirectoryPath() {
            return this.previousTestDirectoryPath;
        }

        public String getNewDirectoryName() {
            return this.newDirectoryName;
        }
    }

    private static void copyGalleryLib() {
        Path galleryLib = getProjectReportFolder().resolve("gallery-lib");
        if (!Files.exists(galleryLib)) {
            try {
                InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(GALLERY_ZIP);
                if (is == null) {
                    System.out.println("Unable to find in classpath: " + GALLERY_ZIP);
                    return;
                }
                ZipManager.copyInputStream(is,
                        new BufferedOutputStream(new FileOutputStream(getProjectReportFolder().resolve(GALLERY_ZIP).toFile())));
                ZipManager.unzip(getProjectReportFolder().resolve(GALLERY_ZIP).toFile().toString(), getProjectReportFolder().toString());
                Files.delete(getProjectReportFolder().resolve(GALLERY_ZIP));
            } catch (Exception e) {
                System.out.println("Unable to copyGalleryLib! Message: " + e.getMessage());
            }
        }
    }

    /**
     * Creates base directory for tests execution to save screenshots, logs etc
     * 
     * @deprecated use {@link #getBaseDirectory()} instead
     * @return base root folder for run.
     */
    @Deprecated(forRemoval = true, since = "1.2.6")
    public static File getBaseDir() {
        try {
            return BASE_DIRECTORY_INITIALIZER.get()
                    .toFile();
        } catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Creates temp directory for tests execution
     * 
     * @deprecated use {@link #getTempDirectory()} instead
     * @return temp folder for run.
     */
    @Deprecated(forRemoval = true, since = "1.2.6")
    public static synchronized File getTempDir() {
        return getTempDirectory().toFile();
    }

    /**
     * Creates unique test directory for test
     * 
     * @deprecated use {@link #getTestDirectory()} instead
     * @return test log/screenshot folder.
     */
    @Deprecated(forRemoval = true, since = "1.2.6")
    public static File getTestDir() {
        return getTestDirectory().toFile();
    }

    /**
     * Rename test directory to custom name.
     * 
     * @deprecated use {@link #renameTestDirectory(String)} instead
     * @param dirName String
     * @return test report dir
     */
    @Deprecated(forRemoval = true, since = "1.2.6")
    public static synchronized File setCustomTestDirName(String dirName) {
        return renameTestDirectory(dirName).toFile();
    }

    /**
     * @deprecated use {@link #getTestDirectory()} instead
     */
    @Deprecated(forRemoval = true, since = "1.2.6")
    public static synchronized File createTestDir() {
        return initTestDirectory().toFile();
    }

    /**
     * @deprecated move to the places where it used (carina-webdriver)
     */
    @Deprecated(forRemoval = true, since = "1.2.6")
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
