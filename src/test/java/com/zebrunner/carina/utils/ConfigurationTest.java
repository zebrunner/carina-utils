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

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.StandardConfigurationOption;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.zebrunner.carina.utils.commons.SpecialKeywords;

/**
 * Tests for {@link Configuration}
 */
public class ConfigurationTest {

    @Test
    public void testConfigOverride() {
        R.CONFIG.put("env", "UNITTEST", true);
        Assert.assertEquals(Configuration.getRequired("override", String.class, StandardConfigurationOption.ENVIRONMENT), "override_me");
        R.CONFIG.put("UNITTEST.override", "i_am_overriden", true);
        Assert.assertEquals(Configuration.getRequired("override", String.class, StandardConfigurationOption.ENVIRONMENT), "i_am_overriden");
    }

    @Test
    public void testGetEnvArg() {
        R.CONFIG.put("env", "QA", true);
        Assert.assertEquals(Configuration.getRequired("url", String.class, StandardConfigurationOption.ENVIRONMENT), "local");
        R.CONFIG.put("env", "PROD", true);
        Assert.assertEquals(Configuration.getRequired("url", String.class, StandardConfigurationOption.ENVIRONMENT), "remote");
    }

    @Test
    public void testConfigurationPlacehodler() {
        R.CONFIG.put("env", "STG", true);
        Assert.assertEquals(Configuration.getRequired("url", String.class, StandardConfigurationOption.ENVIRONMENT), "http://localhost:8081");
    }

    @Test
    public void testAdbExecTimeout() {
        R.CONFIG.put(SpecialKeywords.ADB_EXEC_TIMEOUT, "30000", true);
        Assert.assertEquals(Configuration.getRequired(SpecialKeywords.ADB_EXEC_TIMEOUT, Integer.class), 30000,
                "capabilities.adbExecTimeout wasn't set");
    }

    @Test
    public void testPlatformVersion() {
        R.CONFIG.put(SpecialKeywords.PLATFORM_VERSION, "11.0.0", true);
        Assert.assertEquals(Configuration.getRequired(SpecialKeywords.PLATFORM_VERSION, String.class), "11.0.0",
                "capabilities.platformVersion wasn't set");
    }

    @Test
    public void testBrowser() {
        R.CONFIG.put("browser", "firefox", true);
        Assert.assertEquals(Configuration.getRequired("browser", String.class), "firefox", "browser wasn't set");
    }

    @Test
    public void testBrowserVersion() {
        R.CONFIG.put("capabilities.browserVersion", "88.0.0", true);
        Assert.assertEquals(Configuration.getRequired("capabilities.browserVersion", String.class), "88.0.0",
                "capabilities.browserVersion wasn't set");
    }

}
