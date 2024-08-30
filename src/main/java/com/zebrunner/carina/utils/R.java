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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;

/**
 * R - loads properties from resource files.
 *
 * @author Aliaksei_Khursevich
 *         <a href="mailto:hursevich@gmail.com">Aliaksei_Khursevich</a>
 *
 */
public enum R {
    API("api.properties"),

    CONFIG("config.properties"),

    TESTDATA("testdata.properties"),

    REPORT("report.properties"),

    DATABASE("database.properties"),

    ZAFIRA("zafira.properties");

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OVERRIDE_SIGN = "_";

    private final String resourceFile;

    /**
     * With some Maven plugins usage (e.g. exec-maven-plugin) SystemClassLoader can
     * not 'see' project resources. That's why we need a possibility to choose what
     * ClassLoader instance to use.
     * todo think about refactoring this code
     */
    private static final LazyInitializer<ClassLoader> CLASS_LOADER = new LazyInitializer<>() {
        @Override
        protected ClassLoader initialize() {
            String carinaClassLoader = System.getProperty("carina_class_loader");
            if (!StringUtils.isEmpty(carinaClassLoader) && Boolean.valueOf(carinaClassLoader)) {
                return R.class.getClassLoader();
            }
            return ClassLoader.getSystemClassLoader();
        }
    };

    // temporary thread/test properties which is cleaned on afterTest phase for current thread. It can override any value from below R enum maps
    private static ThreadLocal<Properties> testProperties = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> PROPERTY_OVERWRITE_NOTIFICATIONS = new ThreadLocal<>();

    private static Map<String, Properties> defaultPropertiesHolder = new HashMap<>();
    // permanent global configuration map
    private static Map<String, Properties> propertiesHolder = new HashMap<>();

    // init global configuration map statically
    static {
        reinit();
    }

    /**
     * For internal usage only!
     */
    public static ClassLoader getClassLoader() {
        try {
            return CLASS_LOADER.get();
        }catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static void reinit() {
        for (R resource : values()) {
            try {
                Properties properties = new Properties();

                if (isResourceExists(resource.resourceFile)) {
                    Properties collectedProperties = collect(resource.resourceFile);
                    properties.putAll(collectedProperties);
                    defaultPropertiesHolder.put(resource.resourceFile, collectedProperties);
                }

                URL overrideResource;
                StringBuilder resourceNameBuilder = new StringBuilder(OVERRIDE_SIGN + resource.resourceFile);
                while ((overrideResource = CLASS_LOADER.get().getResource(resourceNameBuilder.toString())) != null) {
                    try (InputStream resourceStream = overrideResource.openStream()) {
                        properties.load(resourceStream);
                        resourceNameBuilder.insert(0, OVERRIDE_SIGN);
                    }
                }

                // Overrides properties by env variables
                for (Object key : properties.keySet()) {
                    String systemValue = System.getenv((String) key);
                    if (!StringUtils.isEmpty(systemValue)) {
                        properties.put(key, systemValue);
                    }
                }

                // Overrides properties by systems properties (java arguments)
                for (Object key : properties.keySet()) {
                    String systemValue = System.getProperty((String) key);
                    if (!StringUtils.isEmpty(systemValue)) {
                        properties.put(key, systemValue);
                    }
                }
                if (resource.resourceFile.contains(CONFIG.resourceFile)) {
                    // no need to read env variables using System.getenv()
                    final String prefix = SpecialKeywords.CAPABILITIES + ".";

                    // read all java arguments and redefine capabilities.* items
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, String> javaProperties = new HashMap(System.getProperties());
                    for (Map.Entry<String, String> entry : javaProperties.entrySet()) {
                        String key = entry.getKey();
                        if (key.toLowerCase().startsWith(prefix)) {
                            String value = entry.getValue();
                            if (!StringUtils.isEmpty(value) && !value.equalsIgnoreCase(SpecialKeywords.NULL)) {
                                properties.put(key, value);
                            }
                        }
                    }
                    // delete all empty or null capabilites.* items from properties
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        String key = (String) entry.getKey();
                        String value = (String) entry.getValue();
                        if (key.toLowerCase().startsWith(prefix) &&
                                (StringUtils.isBlank(value) || value.equalsIgnoreCase(SpecialKeywords.NULL))) {
                            properties.remove(key, value);

                        }
                    }
                }
                propertiesHolder.put(resource.resourceFile, properties);
            } catch (Exception e) {
                throw new InvalidConfigurationException("Invalid config in '" + resource + "': " + e.getMessage());
            }
        }
    }

    /**
     * Checks for the presence of at least one resource in the classpath
     * 
     * @param resourceName the name of the resource being searched for
     * @return true if at least one resource found, false otherwise
     */
    private static boolean isResourceExists(String resourceName) {
        try {
            return CLASS_LOADER.get().getResource(resourceName) != null;
        }catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Collect all properties with the same name into a single Properties object
     * 
     * @param resourceName resource name, for example config.properties
     * @return collected properties
     */
    private static Properties collect(String resourceName) throws IOException {
        try {
            Properties assembledProperties = new Properties();
            Enumeration<URL> resourceURLs = CLASS_LOADER.get().getResources(resourceName);
            while (resourceURLs.hasMoreElements()) {
                Properties tempProperties = new Properties();
                URL url = resourceURLs.nextElement();

                try (InputStream stream = url.openStream()) {
                    tempProperties.load(stream);
                    assembledProperties.putAll(tempProperties);
                }
            }
            return assembledProperties;
        }catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    R(String resourceKey) {
        this.resourceFile = resourceKey;
    }

    /**
     * Compares the current value of property with the default value
     * 
     * @param key name of property
     * @return true if current value equals default value, otherwise false
     */
    public boolean isOverwritten(String key) {
        String currentValue = get(key);
        String defaultValue = defaultPropertiesHolder.get(resourceFile).getProperty(key);
        if (defaultValue == null) {
            defaultValue = StringUtils.EMPTY;
        }

        return !currentValue.equals(defaultValue);
    }

    /**
     * Put and update globally value for properties context.
     * 
     * @param key String
     * @param value String
     */
    public void put(String key, String value) {
        put(key, value, false);
    }

    /**
     * Put and update globally or for current test only value for properties context.
     * 
     * @param key String
     * @param value String
     * @param currentTestOnly boolean
     */
    public void put(String key, String value, boolean currentTestOnly) {
        if (currentTestOnly) {

            LOGGER.warn("Override property for current test '{}={}'!", key, value);

            // declare temporary property key
            getTestProperties().put(key, value);
        } else {
            // override globally configuration map property
            propertiesHolder.get(resourceFile).put(key, value);
        }
    }

    /**
     * Verify if key is declared in data map.
     * 
     * @param key name to verify
     * @return boolean
     */
    public boolean containsKey(String key) {
        return propertiesHolder.get(resourceFile).containsKey(key) || getTestProperties().containsKey(key);
    }

    /**
     * Return value either from system properties or config properties context.
     * System properties have higher priority.
     * Decryption is not performed!
     * 
     * @param key Requested key
     * @return config value
     */
    public String get(String key) {
        String value = getTestProperties().getProperty(key);
        if (value != null) {
            if (PROPERTY_OVERWRITE_NOTIFICATIONS.get() == null) {
                PROPERTY_OVERWRITE_NOTIFICATIONS.set(new HashMap<>());
            }
            // do not warn user about this system property update
            if (!(PROPERTY_OVERWRITE_NOTIFICATIONS.get().containsKey(key) &&
                            value.equals(PROPERTY_OVERWRITE_NOTIFICATIONS.get().get(key)))) {
                LOGGER.warn("Overridden '{}={}' property will be used for current test!", key, value);
                PROPERTY_OVERWRITE_NOTIFICATIONS.get().put(key, value);
            }
            return value;
        }

        value = CONFIG.resourceFile.equals(resourceFile) ? PlaceholderResolver.resolve(propertiesHolder.get(resourceFile), key)
                : propertiesHolder.get(resourceFile).getProperty(key);

        // [VD] Decryption is prohibited here otherwise we have plain sensitive information in logs!

        // [VD] as designed empty MUST be returned
        return value != null ? value : StringUtils.EMPTY;
    }

    /**
     * Return decrypted value either from system properties or config properties context.
     * System properties have higher priority.
     * Decryption is performed if required.
     * 
     * @param key Requested key
     * @return config value
     */
    public String getDecrypted(String key) {
        return EncryptorUtils.decrypt(get(key));
    }

    /**
     * Return Integer value either from system properties or config properties context.
     * 
     * @param key Requested key
     * @return value Integer
     */
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    /**
     * Return long value either from system properties or config properties context.
     * 
     * @param key Requested key
     * @return value long
     */
    public long getLong(String key) {
        return Long.parseLong(get(key));
    }

    /**
     * Return Double value either from system properties or config properties context.
     * 
     * @param key Requested key
     * @return value Double
     */
    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    /**
     * Return boolean value either from system properties or config properties context.
     * 
     * @param key Requested key
     * @return value boolean
     */
    public boolean getBoolean(String key) {
        return Boolean.valueOf(get(key));
    }

    public static String getResourcePath(String resource) {
        try {
            String path = StringUtils.removeStart(CLASS_LOADER.get().getResource(resource).getPath(), "/");
            path = StringUtils.replaceChars(path, "/", "\\");
            path = StringUtils.replaceChars(path, "!", "");
            return path;
        }catch (ConcurrentException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public Properties getProperties() {
        Properties globalProp = new Properties();
        globalProp.putAll(propertiesHolder.get(resourceFile));
        // Glodal properties will be updated with test specific properties
        if (!getTestProperties().isEmpty()) {
            Properties testProp = testProperties.get();
            LOGGER.debug("CurrentTestOnly properties has [{}] entries.", testProp.size());
            LOGGER.debug("{}", testProp);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Map<String, String> testCapabilitiesMap = new HashMap(testProp);
            testCapabilitiesMap.keySet().stream().forEach(i -> {
                if (globalProp.containsKey(i)) {
                    LOGGER.debug(String.format(
                            "Global properties already contains key --- %s --- with value --- %s ---. Global property will be overridden by  --- %s --- from test properties.",
                            i, globalProp.get(i), testProp.get(i)));
                } else {
                    LOGGER.debug(String.format(
                            "Global properties isn't contains key --- %s ---.  Global key --- %s --- will be set to --- %s ---  from test properties.",
                            i, i, testProp.get(i)));
                }
                globalProp.setProperty(i, (String) testProp.get(i));
            });
        }
        return globalProp;
    }

    public void clearTestProperties() {
        testProperties.remove();
        PROPERTY_OVERWRITE_NOTIFICATIONS.remove();
    }

    public Properties getTestProperties() {
        if (testProperties.get() == null) {
            // init temporary properties at first call
            Properties properties = new Properties();
            testProperties.set(properties);
        }

        return testProperties.get();
    }

}
