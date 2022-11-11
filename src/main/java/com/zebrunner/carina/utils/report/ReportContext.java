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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.imgscalr.Scalr;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.decorators.Decorated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.FileManager;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.ZipManager;
import com.zebrunner.carina.utils.common.CommonUtils;
import com.zebrunner.carina.utils.commons.SpecialKeywords;

/*
 * Be careful with LOGGER usage here because potentially it could do recursive call together with ThreadLogAppender functionality
 */

public class ReportContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ROOT_DIR_SYSTEM_PROPERTY = "user.dir";
    private static final String FOLDERS_FORMAT = "%s/%s";
    private static final String SPACE_PATTERN = "[^a-zA-Z0-9.-]";
    public static final String ARTIFACTS_FOLDER = "downloads"; // renamed to downloads to avoid automatic upload on our old Zebrunner ci-pipeline
                                                               // versions

    private static final String GALLERY_ZIP = "gallery-lib.zip";
    private static final String REPORT_NAME = "/report.html";
    private static final int MAX_IMAGE_TITLE = 300;
    private static final String TITLE = "Test steps demo";

    public static final String TEMP_FOLDER = "temp";

    private static File baseDirectory = null;

    private static File tempDirectory;

    private static long rootID;

    private static final ThreadLocal<File> testDirectory = new InheritableThreadLocal<>();
    private static final ThreadLocal<Boolean> isCustomTestDirName = new InheritableThreadLocal<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    // Collects screenshot comments. Screenshot comments are associated using screenshot file name.
    private static final Map<String, String> screenSteps = Collections.synchronizedMap(new HashMap<>());

    private ReportContext() {
    }

    /**
     * Creates base directory for tests execution to save screenshots, logs etc
     * 
     * @return base root folder for run.
     */
    public static File getBaseDir() {
        if (baseDirectory == null) {
            removeOldReports();
            File projectRoot = new File(String.format(FOLDERS_FORMAT, URLDecoder.decode(System.getProperty(ROOT_DIR_SYSTEM_PROPERTY),
                            StandardCharsets.UTF_8),
                    Configuration.get(Configuration.Parameter.PROJECT_REPORT_DIRECTORY)));
            if (!projectRoot.exists()) {
                boolean isCreated = projectRoot.mkdirs();
                if (!isCreated) {
                    throw new RuntimeException("Folder not created: " + projectRoot.getAbsolutePath());
                }
            }
            rootID = System.currentTimeMillis();
            String directory = String.format("%s/%s/%d", URLDecoder.decode(System.getProperty(ROOT_DIR_SYSTEM_PROPERTY), StandardCharsets.UTF_8),
                    Configuration.get(Configuration.Parameter.PROJECT_REPORT_DIRECTORY), rootID);
            File baseDirectoryTmp = new File(directory);
            boolean isCreated = baseDirectoryTmp.mkdir();
            if (!isCreated) {
                throw new RuntimeException("Folder not created: " + baseDirectory.getAbsolutePath());
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

    public static synchronized File getArtifactsFolder() {
        File dir = null;
        try {
            // artifacts directory should use canonical path otherwise auto download feature is broken in browsers
            if (!Configuration.get(Configuration.Parameter.CUSTOM_ARTIFACTS_FOLDER).isEmpty()) {
                dir = new File(Configuration.get(Configuration.Parameter.CUSTOM_ARTIFACTS_FOLDER)).getCanonicalFile();
            } else {
                dir = new File(getTestDir().getCanonicalPath() + File.separator + ARTIFACTS_FOLDER);
            }

            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new RuntimeException("Artifacts folder not created: " + dir.getAbsolutePath());
                } else {
                    LOGGER.debug("Artifacts folder created: {}",  dir.getAbsolutePath());
                }
            } else {
                LOGGER.debug("Artifacts folder already exists: {}", dir.getAbsolutePath());
            }

            if (!dir.isDirectory()) {
                throw new RuntimeException("Artifacts folder is not a folder: " + dir.getAbsolutePath());
            }

        } catch (IOException e) {
            throw new RuntimeException("Artifacts folder not created!");
        }
        return dir;
    }

    /**
     * Returns consolidated list of auto downloaded filenames from local artifacts folder or from remote Selenium session
     * 
     * @param driver WebDriver
     * @return list of file and directories names
     */
    public static List<String> listArtifacts(WebDriver driver) {
        List<String> artifactNames = Arrays.stream(Objects.requireNonNull(getArtifactsFolder().listFiles()))
                .map(File::getName)
                .collect(Collectors.toList());

        String hostUrl = getUrl(driver, "");
        String username = getField(hostUrl, 1);
        String password = getField(hostUrl, 2);

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(hostUrl).openConnection();
            con.setInstanceFollowRedirects(true); // explicitly define as true because default value doesn't work and return 301 status
            con.setRequestMethod("GET");

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }

            int responseCode = con.getResponseCode();
            try (InputStream connectionStream = con.getInputStream()) {
                String responseBody = readStream(connectionStream);
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND &&
                        responseBody.contains("\"error\":\"invalid session id\",\"message\":\"unknown session")) {
                    throw new RuntimeException("Invalid session id. Something wrong with driver");
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String hrefAttributePattern = "href=([\"'])((?:(?!\\1)[^\\\\]|(?:\\\\\\\\)*\\\\[^\\\\])*)\\1";
                    Pattern pattern = Pattern.compile(hrefAttributePattern);
                    Matcher matcher = pattern.matcher(responseBody);
                    while (matcher.find()) {
                        if (!artifactNames.contains(matcher.group(2))) {
                            artifactNames.add(matcher.group(2));
                        }
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.debug("Something went wrong when try to get artifacts from remote", e);
        }

        return artifactNames;
    }

    /**
     * Get artifacts from auto download folder of local or remove driver session by pattern
     * 
     * @param driver WebDriver
     * @param pattern String - regex for artifacts
     * @return list of artifact files
     */
    public static List<File> getArtifacts(WebDriver driver, String pattern) {
        List<String> filteredFilesNames = listArtifacts(driver)
                .stream()
                // ignore directories
                .filter(fileName -> !fileName.endsWith("/"))
                .filter(fileName -> fileName.matches(pattern))
                .collect(Collectors.toList());

        List<File> artifacts = new ArrayList<>();

        for (String fileName : filteredFilesNames) {
            artifacts
                    .add(getArtifact(driver, fileName));
        }
        return artifacts;
    }

    /**
     * Get artifact from auto download folder of local or remove driver session by name
     * 
     * @param driver WebDriver
     * @param name String - filename with extension
     * @return artifact File
     */
    public static File getArtifact(WebDriver driver, String name) {
        File file = new File(getArtifactsFolder() + File.separator + name);
        if (file.exists()) {
            return file;
        }

        String path = file.getAbsolutePath();
        LOGGER.debug("artifact file to download: {}", path);

        String url = getUrl(driver, name);
        String username = getField(url, 1);
        String password = getField(url, 2);

        if (!username.isEmpty() && !password.isEmpty()) {
            Authenticator.setDefault(new CustomAuthenticator(username, password));
        }

        if (checkArtifactUsingHttp(url, username, password)) {
            try {
                FileUtils.copyURLToFile(new URL(url), file);
                LOGGER.debug("Successfully downloaded artifact: {}", name);
            } catch (IOException e) {
                LOGGER.error("Artifact: {} wasn't downloaded to {}",url, path, e);
            }
        } else {
            Assert.fail("Unable to find artifact: " + name);
        }

        // publish as test artifact to Zebrunner Reporting
        try {
            Class<?> artifactClass = ClassUtils.getClass("com.zebrunner.agent.core.registrar.Artifact");
            MethodUtils.invokeStaticMethod(artifactClass, "attachToTest", name, file);
        } catch (Exception e) {
            LOGGER.debug("Cannot attach artifact to the test.", e);
        }
        return file;
    }

    /**
     * check if artifact exists using http
     * 
     * @param url String
     * @param username String
     * @param password String
     * @return boolean
     */
    private static boolean checkArtifactUsingHttp(String url, String username, String password) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            // HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");

            if (!username.isEmpty() && !password.isEmpty()) {
                String usernameColonPassword = username + ":" + password;
                String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                con.addRequestProperty("Authorization", basicAuthPayload);
            }

            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            LOGGER.debug("Artifact doesn't exist: " + url, e);
            return false;
        }
    }

    /**
     * get username or password from url
     * 
     * @param url String
     * @param position int
     * @return String
     */
    private static String getField(String url, int position) {
        Pattern pattern = Pattern.compile(".*:\\/\\/(.*):(.*)@");
        Matcher matcher = pattern.matcher(url);

        return matcher.find() ? matcher.group(position) : "";

    }

    /**
     * Generate file in artifacts location and register in Zebrunner Reporting
     * 
     * @param name String
     * @param source InputStream
     */
    public static void saveArtifact(String name, InputStream source) throws IOException {
        File artifact = new File(String.format(FOLDERS_FORMAT, getArtifactsFolder(), name));
        boolean isSuccessful = artifact.createNewFile();
        if (!isSuccessful) {
            LOGGER.debug("Artifact already exists!");
        }
        FileUtils.writeByteArrayToFile(artifact, IOUtils.toByteArray(source));
        try {
            Class<?> artifactClass = ClassUtils.getClass("com.zebrunner.agent.core.registrar.Artifact");
            MethodUtils.invokeStaticMethod(artifactClass, "attachToTest", name, IOUtils.toByteArray(source));
        } catch (Exception e) {
            LOGGER.debug("Cannot attach artifact to the test.", e);
        }
    }

    /**
     * Copy file into artifacts location and register in Zebrunner Reporting
     * 
     * @param source File
     */

    public static void saveArtifact(File source) throws IOException {
        File artifact = new File(String.format(FOLDERS_FORMAT, getArtifactsFolder(), source.getName()));
        boolean isSuccessful = artifact.createNewFile();
        if (!isSuccessful) {
            LOGGER.debug("Artifact already exists!");
        }
        FileUtils.copyFile(source, artifact);
        try {
            Class<?> artifactClass = ClassUtils.getClass("com.zebrunner.agent.core.registrar.Artifact");
            MethodUtils.invokeStaticMethod(artifactClass, "attachToTest", source.getName(), artifact);
        } catch (Exception e) {
            LOGGER.debug("Cannot attach artifact to the test.", e);
        }
    }

    /**
     * generate url for artifact by name
     * 
     * @param driver WebDriver
     * @param name String
     * @return String
     */
    private static String getUrl(WebDriver driver, String name) {
        String seleniumHost = Configuration.getSeleniumUrl().replace("wd/hub", "download/");
        RemoteWebDriver drv = driver instanceof Decorated ? (RemoteWebDriver) (((Decorated<WebDriver>) driver).getOriginal())
                : (RemoteWebDriver) driver;
        String sessionId = drv.getSessionId().toString();
        String url = seleniumHost + sessionId + "/" + name;
        LOGGER.debug("url: {}", url);
        return url;
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

    /**
     * Removes emailable html report and oldest screenshots directories according to history size defined in config.
     */
    private static void removeOldReports() {
        File baseDir = new File(String.format(FOLDERS_FORMAT, System.getProperty(ROOT_DIR_SYSTEM_PROPERTY),
                Configuration.get(Configuration.Parameter.PROJECT_REPORT_DIRECTORY)));

        if (baseDir.exists()) {
            // remove old emailable report
            File reportFile = new File(String.format("%s/%s/%s", System.getProperty(ROOT_DIR_SYSTEM_PROPERTY),
                    Configuration.get(Configuration.Parameter.PROJECT_REPORT_DIRECTORY), SpecialKeywords.HTML_REPORT));
            if (reportFile.exists()) {
                boolean isSuccessful = reportFile.delete();
                if (!isSuccessful) {
                    LOGGER.debug("Something went wrong when try to delete  '{}' report file", reportFile.getAbsolutePath());
                }
                try {
                    Files.delete(reportFile.toPath());
                } catch (IOException e) {
                    System.out.println((e + "\n" + e.getMessage()));
                }
            }

            List<File> files = FileManager.getFilesInDir(baseDir);
            List<File> screenshotFolders = new ArrayList<>();
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    screenshotFolders.add(file);
                }
            }

            int maxHistory = Configuration.getInt(Configuration.Parameter.MAX_SCREENSHOOT_HISTORY);

            if (maxHistory > 0 && screenshotFolders.size() + 1 > maxHistory) {
                Comparator<File> comp = (file1, file2) -> file2.getName().compareTo(file1.getName());
                screenshotFolders.sort(comp);
                for (int i = maxHistory - 1; i < screenshotFolders.size(); i++) {
                    if (screenshotFolders.get(i).getName().equals("gallery-lib")) {
                        continue;
                    }
                    try {
                        FileUtils.deleteDirectory(screenshotFolders.get(i));
                    } catch (IOException e) {
                        System.out.println((e + "\n" + e.getMessage()));
                    }
                }
            }
        }
    }

    public static void generateHtmlReport(String content) {
        String emailableReport = SpecialKeywords.HTML_REPORT;

        File reportFile = new File(String.format("%s/%s/%s", System.getProperty(ROOT_DIR_SYSTEM_PROPERTY),
                Configuration.get(Configuration.Parameter.PROJECT_REPORT_DIRECTORY), emailableReport));
        File reportFileToBaseDir = new File(String.format(FOLDERS_FORMAT, getBaseDir(), emailableReport));

        try (FileWriter reportFileWriter = new FileWriter(reportFile.getAbsoluteFile());
             BufferedWriter reportBufferedWriter = new BufferedWriter(reportFileWriter);
             FileWriter baseDirFileWriter = new FileWriter(reportFileToBaseDir.getAbsolutePath());
             BufferedWriter baseDirBufferedWriter = new BufferedWriter(baseDirFileWriter)) {

            createNewFileIfNotExists(reportFile);
            reportBufferedWriter.write(content);

            createNewFileIfNotExists(reportFileToBaseDir);
            baseDirBufferedWriter.write(content);

        } catch (IOException e) {
            LOGGER.error("generateHtmlReport failure", e);
        }
    }

    private static void createNewFileIfNotExists(File file) throws IOException {
        if (!file.exists()) {
            boolean isCreated = file.createNewFile();
            if (!isCreated) {
                throw new RuntimeException("File not created: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Returns URL for test artifacts folder.
     * 
     * @return - URL for test screenshot folder.
     */
    public static String getTestArtifactsLink() {
        String link = "";
        if (!Configuration.get(Configuration.Parameter.REPORT_URL).isEmpty()) {
            link = String.format("%s/%d/artifacts", Configuration.get(Configuration.Parameter.REPORT_URL), rootID);
        } else {
            link = String.format("file://%s/artifacts", getBaseDirAbsolutePath());
        }

        return link;

    }

    /**
     * Returns URL for test screenshot folder.
     * 
     * @return - URL for test screenshot folder.
     */
    public static String getTestScreenshotsLink() {
        String link = "";
        try {
            if (FileUtils.listFiles(ReportContext.getTestDir(), new String[] { "png" }, false).isEmpty()) {
                // no png screenshot files at all
                return link;
            }
        } catch (Exception e) {
            LOGGER.error("Exception during report directory scanning", e);
        }
        
        String test = testDirectory.get().getName().replaceAll(SPACE_PATTERN, "_");
        
        if (!Configuration.get(Configuration.Parameter.REPORT_URL).isEmpty()) {
            link = String.format("%s/%d/%s/report.html", Configuration.get(Configuration.Parameter.REPORT_URL), rootID, test);
        } else {
            link = String.format("file://%s/%s/report.html", getBaseDirAbsolutePath(), test);
        }

        return link;

    }

    /**
     * Returns URL for test log.
     * @return - URL to test log folder.
     */
    public static String getTestLogLink() {
        String link = "";
        File testLogFile = new File(ReportContext.getTestDir() + "/" + "test.log");
        if (!testLogFile.exists()) {
            // no test.log file at all
            return link;
        }

        String test = testDirectory.get().getName().replaceAll(SPACE_PATTERN, "_");
        if (!Configuration.get(Configuration.Parameter.REPORT_URL).isEmpty()) {
            link = String.format("%s/%d/%s/test.log", Configuration.get(Configuration.Parameter.REPORT_URL), rootID, test);
        } else {
            link = String.format("file://%s/%s/test.log", getBaseDirAbsolutePath(), test);
        }

        return link;
    }

    /**
     * Returns URL for cucumber report.
     * 
     * @return - URL to test log folder.
     */
    public static String getCucumberReportLink() {

        String folder = SpecialKeywords.CUCUMBER_REPORT_FOLDER;
        String subFolder = SpecialKeywords.CUCUMBER_REPORT_SUBFOLDER;
        String fileName = SpecialKeywords.CUCUMBER_REPORT_FILE_NAME;

        String link = "";
        if (!Configuration.get(Configuration.Parameter.REPORT_URL).isEmpty()) {
            String reportUrl = Configuration.get(Configuration.Parameter.REPORT_URL);
            if (reportUrl.contains("n/a")) {
                LOGGER.error("Contains n/a. Replace it.");
                reportUrl = reportUrl.replace("n/a", "");
            }
            link = String.format("%s/%d/%s/%s/%s", reportUrl, rootID, folder, subFolder, fileName);
        } else {
            link = String.format("file://%s/%s/%s/%s", getBaseDirAbsolutePath(), folder, subFolder, fileName);
        }

        return link;
    }

    /**
     * Saves screenshot.
     * 
     * @param screenshot - {@link BufferedImage} file to save
     * 
     * @return - screenshot name.
     */
    public static String saveScreenshot(BufferedImage screenshot) {
        long now = System.currentTimeMillis();

        executor.execute(new ImageSaverTask(screenshot, String.format("%s/%d.png", getTestDir().getAbsolutePath(), now),
                Configuration.getInt(Configuration.Parameter.BIG_SCREEN_WIDTH), Configuration.getInt(Configuration.Parameter.BIG_SCREEN_HEIGHT)));

        return String.format("%d.png", now);
    }

    /**
     * Asynchronous image saver task.
     */
    private static class ImageSaverTask implements Runnable {
        private BufferedImage image;
        private String path;
        private Integer width;
        private Integer height;

        public ImageSaverTask(BufferedImage image, String path, Integer width, Integer height) {
            this.image = image;
            this.path = path;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            try {
                if (width > 0 && height > 0) {
                    BufferedImage resizedImage = Scalr.resize(image, Scalr.Method.BALANCED, Scalr.Mode.FIT_TO_WIDTH, width, height,
                            Scalr.OP_ANTIALIAS);
                    if (resizedImage.getHeight() > height) {
                        resizedImage = Scalr.crop(resizedImage, resizedImage.getWidth(), height);
                    }
                    ImageIO.write(resizedImage, "PNG", new File(path));
                } else {
                    ImageIO.write(image, "PNG", new File(path));
                }

            } catch (Exception e) {
                LOGGER.error("Unable to save screenshot: {}", e.getMessage());
            }
        }
    }

    private static void copyGalleryLib() {
        String filesSeparator = FileSystems.getDefault().getSeparator();
        File reportsRootDir = new File(System.getProperty(ROOT_DIR_SYSTEM_PROPERTY) + "/" + Configuration.get(Configuration.Parameter.PROJECT_REPORT_DIRECTORY));
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

    public static void generateTestReport() {
        File testDir = testDirectory.get();
        try {
            List<File> images = FileManager.getFilesInDir(testDir);
            List<String> imgNames = new ArrayList<>();
            for (File image : images) {
                imgNames.add(image.getName());
            }
            imgNames.remove("test.log");
            imgNames.remove("sql.log");
            if (imgNames.isEmpty())
                return;

            Collections.sort(imgNames);

            StringBuilder report = new StringBuilder();
            for (String imgName : imgNames) {
                // convert toString
                String image = R.REPORT.get("image");

                image = image.replace("${image}", imgName);

                String title = getScreenshotComment(imgName);
                if (title == null) {
                    title = "";
                }
                image = image.replace("${title}", StringUtils.substring(title, 0, MAX_IMAGE_TITLE));
                report.append(image);
            }
            String wholeReport = R.REPORT.get("container").replace("${images}", report.toString());
            wholeReport = wholeReport.replace("${title}", TITLE);
            String folder = testDir.getAbsolutePath();
            FileManager.createFileWithContent(folder + REPORT_NAME, wholeReport);
        } catch (Exception e) {
            LOGGER.error("generateTestReport failure", e);
        }
    }

    /**
     * Stores comment for screenshot.
     *
     * @param screenId screenId id
     * @param msg message
     * 
     */
    public static void addScreenshotComment(String screenId, String msg) {
        if (!StringUtils.isEmpty(screenId)) {
            screenSteps.put(screenId, msg);
        }
    }

    /**
     * Return comment for screenshot.
     * 
     * @param screenId Screen Id
     * 
     * @return screenshot comment
     */
    public static String getScreenshotComment(String screenId) {
        String comment = "";
        if (screenSteps.containsKey(screenId))
            comment = screenSteps.get(screenId);
        return comment;
    }

    private static String getBaseDirAbsolutePath() {
        if (baseDirectory != null) {
            return baseDirectory.getAbsolutePath();
        }
        return null;
    }

    // Converting InputStream to String
    private static String readStream(InputStream in) {
        StringBuilder response = new StringBuilder();
        try (
                InputStreamReader istream = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(istream)) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            // do noting
        }
        return response.toString();
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
