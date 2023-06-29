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
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.PlaceholderResolverException;

/**
 * PlaceholderResolver - resolves placeholders in properties.
 * 
 * @author Alexey Khursevich (hursevich@gmail.com)
 */
public final class PlaceholderResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Pattern PATTERN = Pattern.compile(SpecialKeywords.PLACEHOLER);

    private PlaceholderResolver() {
    }

    /**
     * Resolves value by placeholder recursively.
     * 
     * @param properties Properties
     * @param key Key
     * @return resolved value
     */
    public static String resolve(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            Matcher matcher = PATTERN.matcher(value);
            while (matcher.find()) {
                String placeholder = matcher.group();
                String placeholderKey = placeholder.replace("${", "").replace("}", "");
                String resolvedValue = resolve(properties, placeholderKey);
                if (resolvedValue != null) {
                    value = value.replace(placeholder, resolvedValue);
                }
            }
        }
        return value;
    }

    /**
     * Verifies that properties file contains all placeholder definitions and does not have infinit placeholder loops.
     * 
     * @param properties Properties value
     * @return validation results
     */
    public static boolean isValid(Properties properties) {
        Set<Object> keys = properties.keySet();
        for (Object key : keys) {
            try {
                resolve(properties, (String) key);
            } catch (StackOverflowError e) {
                LOGGER.error("Infinit placeholder loop was found for '{}'", properties.getProperty((String) key));
                return false;
            } catch (PlaceholderResolverException e) {
                LOGGER.error(e.getMessage());
                return false;
            }
        }
        return true;
    }
}
