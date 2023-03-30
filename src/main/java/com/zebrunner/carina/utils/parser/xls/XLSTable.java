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

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XLSTable extends AbstractTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FK_PREFIX = "FK_LINK_";

    public XLSTable() {
        super();
    }

    public XLSTable(String executeColumn, String executeValue) {
        super(executeColumn, executeValue);
    }

    public void setHeaders(Row row) {
        headers.clear();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            headers.add(XLSParser.getCellValue(row.getCell(i)));
        }
    }

    public void addDataRow(Row row, Workbook wb, Sheet sheet) {
        if (row == null) {
            // don't add any data row if it is null. It seems like there is empty row in xls file
            return;
        }
        addDataRow(rowIndex -> XLSParser.getCellValue(row.getCell(rowIndex)), row, wb, sheet);
    }

    @Override
    public void addDataRow(List<String> row) {
        if (row == null) {
            return;
        }
        addDataRow(index -> row.size() > index ? row.get(index) : null, null, null, null);
    }

    private void addDataRow(Function<Integer, String> cellValueGetter, Row row, Workbook wb, Sheet sheet) {
        if ((executeColumn != null && executeValue != null && headers.contains(executeColumn))
                && (!executeValue.equalsIgnoreCase(cellValueGetter.apply(headers.indexOf(executeColumn))))) {
                return;
        }

        XLSChildTable childRow = null;

        Map<String, String> dataMap = new HashMap<>();
        LOGGER.debug("Loading data from row: ");
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header.startsWith(FK_PREFIX)) {
                if (row != null && wb != null && sheet != null) {
                    childRow = XLSParser.parseCellLinks(row.getCell(i), wb, sheet);
                } else {
                    LOGGER.warn("FK_LINK_ prefix is not currently supported for spreadsheets");
                }
            }

            dataMap.put(header, cellValueGetter.apply(i));
            LOGGER.debug("{}:  {}", header, dataMap.get(header));
        }

        // If row has foreign key than merge headers and data
        merge(childRow, dataMap);

        LOGGER.debug("Merged row: ");
        for (String header : headers) {
            LOGGER.debug("{}: {}", header, dataMap.get(header));
        }

        dataRows.add(dataMap);
    }

    private void merge(XLSChildTable childRow, Map<String, String> dataMap) {
        if (childRow != null) {
            LOGGER.debug("Loading data from child row: ");
            for (int i = 0; i < childRow.getHeaders().size(); i++) {
                String currentHeader = childRow.getHeaders().get(i);

                if (StringUtils.isBlank(dataMap.get(currentHeader))) {
                    // Merge headers
                    if (!this.headers.contains(currentHeader))
                        this.headers.add(currentHeader);

                    // Merge data
                    dataMap.put(currentHeader, childRow.getDataRows().get(0).get(currentHeader));
                }
                LOGGER.debug("{}: {}", currentHeader, childRow.getDataRows().get(0).get(currentHeader));
            }
        }
    }
}
