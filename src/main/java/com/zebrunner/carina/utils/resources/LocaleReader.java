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
package com.zebrunner.carina.utils.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LocaleReader {

    private LocaleReader() {
    }

    public static List<Locale> init(String locale) {

        List<Locale> locales = new ArrayList<>();
        String[] strLocales = locale.split(",");

        for (String strLocale : strLocales) {
            String[] localeSetttings = strLocale.trim().split("_");
            String lang = "";
            String country = "";
            lang = localeSetttings[0];
            if (localeSetttings.length > 1) {
                country = localeSetttings[1];
            }
            locales.add(new Locale(lang, country));
        }

        return locales;
    }

}
