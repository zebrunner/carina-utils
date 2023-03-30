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
package com.zebrunner.carina.utils.parser.xls;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class XLSCache {
    private static final Map<String, Workbook> xlsCache = new HashMap<>();

    private XLSCache() {
        // do nothing
    }

    public static synchronized Workbook getWorkbook(String xlsPath) {
        if (!xlsCache.containsKey(xlsPath)) {
            Workbook wb;
            try {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(xlsPath)) {
                    wb = WorkbookFactory.create(is);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Can't read xls: " + xlsPath, e);
            }
            xlsCache.put(xlsPath, wb);
        }
        return xlsCache.get(xlsPath);
    }

    public static synchronized String getWorkbookPath(Workbook book) {
        for (Entry<String, Workbook> entry : xlsCache.entrySet()) {
            if (entry.getValue() == book)
                return entry.getKey();
        }
        return null;
    }
}
