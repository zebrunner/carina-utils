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
package com.zebrunner.carina.utils;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.MissingParameterException;

/**
 * Configuration utility.
 * 
 * @deprecated use {@link com.zebrunner.carina.utils.config.Configuration} class instead
 * @author Aliaksei_Khursevich
 *         hursevich@gmail.com
 */
@Deprecated(forRemoval = true, since = "1.0.5")
public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static IEnvArgResolver envArgResolver = new DefaultEnvArgResolver();

    /**
     * All available configuration parameter keys along with default values.
     */
    public enum Parameter {
        URL("url"),

        ENV("env"),

        BROWSER("browser"),

        BROWSER_LANGUAGE("browser_language"),

        SELENIUM_URL("selenium_url"),

        DRIVER_EVENT_LISTENERS("driver_event_listeners"),

        MAX_DRIVER_COUNT("max_driver_count"),

        FORCIBLY_DISABLE_DRIVER_QUIT("forcibly_disable_driver_quit"),

        CUSTOM_CAPABILITIES("custom_capabilities"),

        CHROME_ARGS("chrome_args"),

        CHROME_EXPERIMENTAL_OPTS("chrome_experimental_opts"),

        CHROME_MOBILE_EMULATION_OPTS("chrome_mobile_emulation_opts"),

        CHROME_CLOSURE("chrome_closure"),

        FIREFOX_ARGS("firefox_args"),

        FIREFOX_PREFERENCES("firefox_preferences"),

        HEADLESS("headless"),

        APP_VERSION("app_version"),

        PROXY_HOST("proxy_host"),

        PROXY_PORT("proxy_port"),

        PROXY_PROTOCOLS("proxy_protocols"),

        NO_PROXY("no_proxy"),

        BROWSERUP_PROXY("browserup_proxy"),

        BROWSERUP_PORT("browserup_port"),

        BROWSERUP_PORTS_RANGE("browserup_ports_range"),

        BROWSERUP_MITM("browserup_disabled_mitm"),

        PROXY_SET_TO_SYSTEM("proxy_set_to_system"),

        @Deprecated
        REPORT_URL("report_url"),

        CI_BUILD_URL("ci_build_url"),

        AUTO_SCREENSHOT("auto_screenshot"),

        ERROR_SCREENSHOT("error_screenshot"),

        ALLOW_FULLSIZE_SCREENSHOT("allow_fullsize_screenshot"),

        EXPLICIT_TIMEOUT("explicit_timeout"),

        /**
         * Timeout is seconds to read response from Selenium/Appium.
         */
        READ_TIMEOUT("read_timeout"),

        AUTO_DOWNLOAD("auto_download"),

        AUTO_DOWNLOAD_APPS("auto_download_apps"),

        CUSTOM_ARTIFACTS_FOLDER("custom_artifacts_folder"),

        RETRY_INTERVAL("retry_interval"),

        PROJECT_REPORT_DIRECTORY("project_report_directory"),

        MAX_SCREENSHOOT_HISTORY("max_screen_history"),

        BIG_SCREEN_WIDTH("big_screen_width"),

        BIG_SCREEN_HEIGHT("big_screen_height"),

        INIT_RETRY_COUNT("init_retry_count"),

        INIT_RETRY_INTERVAL("init_retry_interval"),

        RETRY_COUNT("retry_count"),

        LOCALE("locale"),

        THREAD_COUNT("thread_count"),

        DATA_PROVIDER_THREAD_COUNT("data_provider_thread_count"),

        CORE_LOG_LEVEL("core_log_level"),

        LOG_ALL_JSON("log_all_json"),

        DATE_FORMAT("date_format"),

        TIME_FORMAT("time_format"),

        CRYPTO_KEY_VALUE("crypto_key_value"),

        CRYPTO_PATTERN("crypto_pattern"),

        CRYPTO_ALGORITHM("crypto_algorithm"),

        SUITE_NAME("suite_name"),

        TEST_NAMING_PATTERN("test_naming_pattern"),

        ELEMENT_LOADING_STRATEGY("element_loading_strategy"),

        PAGE_OPENING_STRATEGY("page_opening_strategy"),

        /**
         * Specifies whether to search for pages implementations in dependencies.<br>
         * Project: carina-webdriver
         */
        PAGE_RECURSIVE_REFLECTION("page_recursive_reflection"),

        // Amazon
        S3_BUCKET_NAME("s3_bucket_name"),

        S3_REGION("s3_region"),

        ACCESS_KEY_ID("access_key_id"),

        SECRET_KEY("secret_key"),

        // Azure
        AZURE_ACCOUNT_NAME("azure_account_name"),

        AZURE_CONTAINER_NAME("azure_container_name"),

        AZURE_BLOB_URL("azure_blob_url"),

        AZURE_ACCESS_KEY_TOKEN("azure_access_key_token"),

        AZURE_LOCAL_STORAGE("azure_local_storage"),

        // AppCenter token
        APPCENTER_TOKEN("appcenter_token"),

        // For localization parser
        LOCALIZATION_ENCODING("localization_encoding"),

        LOCALIZATION_TESTING("localization_testing"),

        // TLS
        TLS_KEYSECURE_LOCATION("tls_keysecure_location"),

        UNINSTALL_RELATED_APPS("uninstall_related_apps"),

        // For Device default timezone and language
        DEFAULT_DEVICE_TIMEZONE("default_device_timezone"),

        DEFAULT_DEVICE_TIME_FORMAT("default_device_time_format"),

        DEFAULT_DEVICE_LANGUAGE("default_device_language"),

        // Ignore SSL
        IGNORE_SSL("ignore_ssl"),

        // Test Execution Filter rules
        TEST_RUN_RULES("test_run_rules"),

        // Test Rail
        TESTRAIL_ENABLED("testrail_enabled"),

        INCLUDE_ALL("include_all"),

        MILESTONE("milestone"),

        RUN_NAME("run_name"),

        ASSIGNEE("assignee"),

        // sha1
        GIT_HASH("git_hash"),

        LANGUAGE("language"),

        // Database parameters
        DB_URL("db.url"),

        DB_USERNAME("db.username"),

        DB_PASSWORD("db.password");

        private final String key;
        Parameter(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * Returns configuration value from startup properties or from configuration
     * file if not found in startup args.
     *
     * @param param parameter key.
     * @return The parameter value if it is found by key, or "" if not
     */
    public static String get(Parameter param) {
        String value = R.CONFIG.get(param.getKey());
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }

    /**
     * Get decrypted configuration value from startup properties or from configuration
     * file if not found in startup args.
     *
     * @param param parameter key
     * @return the decrypted parameter value if it is found by key, "" otherwise
     */
    public static String getDecrypted(Parameter param) {
        String value = R.CONFIG.getDecrypted(param.getKey());
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }

    public static int getInt(Parameter param) {
        return Integer.valueOf(get(param).trim());
    }

    public static long getLong(Parameter param) {
        return Long.valueOf(get(param).trim());
    }

    public static double getDouble(Parameter param) {
        return Double.valueOf(get(param).trim());
    }

    public static boolean getBoolean(Parameter param) {
        String value = get(param).trim();
        if (value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) {
            return false;
        }
        return Boolean.valueOf(value);
    }

    public static String asString() {
        String lineFormat = "%s=%s%n";
        StringBuilder asString = new StringBuilder();
        asString.append("\n============= Test configuration =============\n");
        for (Parameter param : Parameter.values()) {
            //#1451 hide WARN! Value not resolved by key: azure_container_name
            if (Parameter.AZURE_BLOB_URL.equals(param) && Configuration.get(param).toLowerCase().contains(SpecialKeywords.NULL.toLowerCase())) {
                // do nothing
                continue;
            }
            if (!Parameter.CRYPTO_KEY_VALUE.equals(param) && !Configuration.get(param).isEmpty() && R.CONFIG.isOverwritten(param.getKey())) {
                asString.append(String.format(lineFormat, param.getKey(), Configuration.get(param)));
            }
        }

        // write into the log extra information about selenium_url together with capabilities
        asString.append(String.format(lineFormat, "selenium_url", getSeleniumUrl()));
        asString.append("\n------------- Driver capabilities -----------\n");
        // read all properties from config.properties and use "capabilities.*"
        final String prefix = SpecialKeywords.CAPABILITIES + ".";
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, String> capabilitiesMap = new HashMap(R.CONFIG.getProperties());
        for (Map.Entry<String, String> entry : capabilitiesMap.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(prefix)) {
                asString.append(String.format(lineFormat, entry.getKey(), R.CONFIG.get(entry.getKey())));
            }
        }

        asString.append("================================================\n");
        return asString.toString();
    }

    public static void validateConfiguration() {
        for (Parameter param : Parameter.values()) {
            if (StringUtils.isEmpty(Configuration.get(param)) || SpecialKeywords.MUST_OVERRIDE.equals(Configuration.get(param))) {
                throw new MissingParameterException("Configuration failure: parameter '" + param.getKey() + "' not specified!");
            }
        }
    }

    public static IEnvArgResolver getEnvArgResolver() {
        return envArgResolver;
    }

    /**
     * Get value of configuration parameter in {@link Configuration.Parameter#ENV},
     * for example {@code env.parameter=value}<br>
     * Decryption is applied
     *
     * @param key parameter key, see {@link Parameter}
     * @return the parameter value if it is found, or "" if parameter was not found or it equals {@link SpecialKeywords#NULL}
     */
    public static String getEnvArg(Parameter key) {
        return getEnvArg(key, "");
    }

    /**
     * Get value of custom configuration parameter in {@link Configuration.Parameter#ENV},
     * for example {@code env.parameter=value}<br>
     * Decryption is not applied (initial implementation). If you want to get the decoded value use {@link #getEnvArg(String, String, boolean)}
     * with alias equals {@code ""}
     *
     * @param key custom parameter key
     * @return the parameter value if it is found, or "" if parameter was not found or it equals {@link SpecialKeywords#NULL}
     */
    public static String getEnvArg(String key) {
        return getEnvArg(key, "", false);
    }

    /**
     * Get value of configuration parameter in {@link Configuration.Parameter#ENV} with alias,
     * for example {@code env.alias.parameter=value}<br>
     * Decryption is applied
     *
     * @param key parameter key, see {@link Parameter}
     * @param alias alias in environment
     * @return the parameter value if it is found, or "" if parameter was not found or it equals {@link SpecialKeywords#NULL}
     */
    public static String getEnvArg(Parameter key, String alias) {
        return getEnvArg(key, alias, true);
    }

    /**
     * Get value of custom configuration parameter in {@link Configuration.Parameter#ENV} with alias,
     * for example {@code env.alias.parameter=value}<br>
     * Decryption is applied
     *
     * @param key custom parameter key
     * @param alias alias in environment
     * @return the parameter value if it is found, or "" if parameter was not found or it equals {@link SpecialKeywords#NULL}
     */
    public static String getEnvArg(String key, String alias) {
        return getEnvArg(key, alias, true);
    }

    /**
     * Get configuration value in {@link Configuration.Parameter#ENV} with alias,
     * for example {@code env.alias.parameter=value}
     *
     * @param key parameter key, see {@link Parameter}
     * @param alias alias in environment
     * @param isDeciph apply decryption or not
     * @return the parameter value if it is found, or "" if parameter was not found or it equals {@link SpecialKeywords#NULL}
     */
    public static String getEnvArg(Parameter key, String alias, boolean isDeciph) {
       return getEnvArg(key.getKey(), alias, isDeciph);
    }


    /**
     * Get value of custom configuration parameter in {@link Configuration.Parameter#ENV} with alias,
     * for example {@code env.alias.parameter=value}<br>
     *
     * @param key custom parameter key
     * @param alias alias in environment
     * @param isDeciph apply decryption or not
     * @return the parameter value if it is found, or "" if parameter was not found or it equals {@link SpecialKeywords#NULL}
     */
    public static String getEnvArg(String key, String alias, boolean isDeciph) {
        if (Configuration.isNull(Configuration.Parameter.ENV)) {
            throw new MissingParameterException("Configuration parameter 'env' should be set!");
        }
        Objects.requireNonNull(alias, "Alias must not be null");
        StringBuilder sb = new StringBuilder(Configuration.get(Configuration.Parameter.ENV));
        if (!alias.isEmpty()) {
            sb.append(".")
                    .append(alias);
        }
        sb.append(".")
                .append(key);

        String value = isDeciph ? R.CONFIG.getDecrypted(sb.toString()) : R.CONFIG.get(sb.toString());
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }

    public static boolean isNull(Parameter param) {
        // null is never returned from configuration now so compare with empty string
        return get(param).isEmpty();
    }

    /**
     * Get capabilities.adbExecTimeout from configuration properties.
     * if it is missing return the default value
     * @return int capabilities.adbExecTimeout
     */
    public static int getAdbExecTimeout() {
        // default "capabilities.adbExecTimeout=value" should be used to determine current platform
        int adbExecTimeout = SpecialKeywords.DEFAULT_ADB_EXEC_TIMEOUT;

        // redefine adb exec timeout if capabilities.AdbExecTimeout is available
        if (!R.CONFIG.get(SpecialKeywords.ADB_EXEC_TIMEOUT).isEmpty()) {
            adbExecTimeout = R.CONFIG.getInt(SpecialKeywords.ADB_EXEC_TIMEOUT);
        }

        return adbExecTimeout;
    }

    /**
     * Get platform name from configuration properties
     * 
     * @return platform name, see {@link org.openqa.selenium.Platform}<br>
     *         {@code *} - any platform
     */
    public static String getPlatform() {
        // warning: do not read "os" or "platformName" from driver capabilities as SauceLabs return LINUX instead of ANDROID
        String platform = "*"; // any platform by default

        LOGGER.debug("platform1: {}", platform);
        // redefine platform if os caps is available
        if (!R.CONFIG.get(SpecialKeywords.BROWSERSTACK_PLATFORM_NAME).isEmpty()) {
            platform = R.CONFIG.get(SpecialKeywords.BROWSERSTACK_PLATFORM_NAME);
            LOGGER.debug("platform2: {}", platform);
        }

        // redefine platform if platformName caps is available
        if (!R.CONFIG.get(SpecialKeywords.PLATFORM_NAME).isEmpty()) {
            platform = R.CONFIG.get(SpecialKeywords.PLATFORM_NAME);
            LOGGER.debug("platform3: {}", platform);
        }
        return platform;
    }

    /**
     * Get platform name from configuration properties or MutableCapabilities.
     * 
     * @deprecated this method does not get platform from caps, only from configuration properties
     *             Use {@link #getPlatform()} instead, it will give the same result
     *             If you want to get platform from capabilities only, use {@link MutableCapabilities#getPlatformName()}
     * @param caps MutableCapabilities
     * @return String platform name
     */
    @Deprecated(forRemoval = true, since = "8.0.2")
    public static String getPlatform(MutableCapabilities caps) {
        // any platform by default
        String platform = "*";

        LOGGER.debug("platform1: {}", platform);
        // redefine platform if os caps is available
        if (!R.CONFIG.get(SpecialKeywords.BROWSERSTACK_PLATFORM_NAME).isEmpty()) {
            platform = R.CONFIG.get(SpecialKeywords.BROWSERSTACK_PLATFORM_NAME);
        }
        LOGGER.debug("platform2: {}", platform);

        // redefine platform if platformName caps is available
        if (!R.CONFIG.get(SpecialKeywords.PLATFORM_NAME).isEmpty()) {
            platform = R.CONFIG.get(SpecialKeywords.PLATFORM_NAME);
        }
        LOGGER.debug("platform3: {}", platform);
        LOGGER.debug("platform4: {}", platform);

        return platform;
    }
    
    public static String getPlatformVersion() {
        return getPlatformVersion(new MutableCapabilities());
    }
    
    public static String getPlatformVersion(MutableCapabilities caps) {
        // default "os_version=value" should be used to determine current platform
        String platformVersion = "";

        // redefine platform if os_version caps is available
        if (!R.CONFIG.get(SpecialKeywords.BROWSERSTACK_PLATFORM_VERSION).isEmpty()) {
            platformVersion = R.CONFIG.get(SpecialKeywords.BROWSERSTACK_PLATFORM_VERSION);
        }
        
        // redefine platform if platformVersion caps is available
        if (!R.CONFIG.get(SpecialKeywords.PLATFORM_VERSION).isEmpty()) {
            platformVersion = R.CONFIG.get(SpecialKeywords.PLATFORM_VERSION);
        }
        
        if (caps != null && caps.getCapability("os_version") != null) {
            platformVersion = caps.getCapability("os_version").toString();
        }           
        
        if (caps != null && caps.getCapability("platformVersion") != null) {
            platformVersion = caps.getCapability("platformVersion").toString();
        }        
        
        return platformVersion;
    }

    /**
     * Takes browserName from browser configuration parameter - see {{@link Configuration.Parameter}} or
     * capabilities.browserName (priority)
     * 
     * @return browser name
     */
    public static String getBrowser() {
        String browser = StringUtils.EMPTY;
        if (!Configuration.get(Parameter.BROWSER).isEmpty()) {
            // default "browser=value" should be used to determine current browser
            browser = Configuration.get(Parameter.BROWSER);
        }

        String configBrowserName = getCapability(CapabilityType.BROWSER_NAME);
        // redefine browser if capabilities.browserName is available
        if (!configBrowserName.isEmpty() && !"null".equalsIgnoreCase(configBrowserName)) {
            browser = configBrowserName;
        }
        return browser;
    }

    public static String getBrowserVersion() {
        String browserVersion = StringUtils.EMPTY;
        String configBrowserVersion = getCapability(CapabilityType.BROWSER_VERSION);
        // redefine browserVersion if capabilities.browserVersion is available
        if (!configBrowserVersion.isEmpty() && !"null".equalsIgnoreCase(configBrowserVersion)) {
            browserVersion = configBrowserVersion;
        }
        return browserVersion;
    }

    /**
     * Returns driver type depends on platform and browser
     * 
     * @return driver type
     */
    public static String getDriverType() {

        String platform = getPlatform();
        String browserName = Configuration.getBrowser();

        if (platform.equalsIgnoreCase(SpecialKeywords.ANDROID) ||
                platform.equalsIgnoreCase(SpecialKeywords.IOS) ||
                platform.equalsIgnoreCase(SpecialKeywords.TVOS)) {
            return SpecialKeywords.MOBILE;
        }

        if (!StringUtils.isEmpty(browserName)) {
            return SpecialKeywords.DESKTOP;
        }
        
        if (SpecialKeywords.WINDOWS.equalsIgnoreCase(platform)) {
            return SpecialKeywords.WINDOWS;
        }

        if (SpecialKeywords.MAC.equalsIgnoreCase(platform)) {
            return SpecialKeywords.MAC;
        }
        return SpecialKeywords.DESKTOP;
    }

    public static String getDriverType(MutableCapabilities capabilities) {
        if (capabilities == null) {
            // calculate driver type based on config.properties arguments
            return getDriverType();
        }

        String platform = StringUtils.EMPTY;
        String browserName = getBrowser();
        if (capabilities.getCapability(CapabilityType.PLATFORM_NAME) != null) {
            platform = capabilities.getCapability(CapabilityType.PLATFORM_NAME).toString();
        }

        if (SpecialKeywords.ANDROID.equalsIgnoreCase(platform) ||
                SpecialKeywords.IOS.equalsIgnoreCase(platform) ||
                SpecialKeywords.TVOS.equalsIgnoreCase(platform)) {
            return SpecialKeywords.MOBILE;
        }

        if (!StringUtils.isEmpty(browserName)) {
            return SpecialKeywords.DESKTOP;
        }

        if (SpecialKeywords.WINDOWS.equalsIgnoreCase(platform)) {
            return SpecialKeywords.WINDOWS;
        }

        if (SpecialKeywords.MAC.equalsIgnoreCase(platform)) {
            return SpecialKeywords.MAC;
        }

        // handle use-case when we provide only uuid object among desired capabilities
        if (capabilities.getCapability("udid") != null) {
            LOGGER.debug("Detected MOBILE driver_type by uuid inside capabilities");
            return SpecialKeywords.MOBILE;
        }
        return SpecialKeywords.DESKTOP;
    }

    public static String getMobileApp() {
        // redefine platform if capabilities.app is available
        String mobileApp = "";
        String prefix = SpecialKeywords.CAPABILITIES + ".";
        if (!R.CONFIG.get(prefix + "app").isEmpty()) {
            mobileApp = R.CONFIG.get(prefix + "app");
        }
        return mobileApp;
    }

    public static void setMobileApp(String mobileApp) {
        R.CONFIG.put(SpecialKeywords.CAPABILITIES + ".app", mobileApp);
        LOGGER.info("Updated mobile app: {}", mobileApp);
    }

    public static String getCapability(String name) {
        return R.CONFIG.get("capabilities." + name);
    }
    
    /**
     * Register APP_VERSION number in CONFIG space and as Zebrunner Reporting build number if not empty.
     *
     * @param build String
     */
    public static void setBuild(String build) {
        R.CONFIG.put(Parameter.APP_VERSION.getKey(), build);
        if (!build.isEmpty()) {
            LOGGER.debug("build: {}", build);
            try {
                Class<?> artifactClass = ClassUtils.getClass("com.zebrunner.agent.core.registrar.CurrentTestRun");
                MethodUtils.invokeStaticMethod(artifactClass, "setBuild", build);
            } catch (Exception e) {
                LOGGER.debug("Cannot attach build to the current test run.", e);
            }
        }
    }

    public static String getSeleniumUrl() {
        return Configuration.get(Parameter.SELENIUM_URL);
    }

    public static int getThreadCount() {
        return Configuration.getInt(Parameter.THREAD_COUNT);
    }

    public static int getDataProviderThreadCount() {
        return Configuration.getInt(Parameter.DATA_PROVIDER_THREAD_COUNT);
    }

}
