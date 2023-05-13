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
package com.zebrunner.carina.utils.db.mongo;

import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

/**
 * Tests for {@link MongoConnector}
 */
public class MongoConnectorTest {
    @Test(expectedExceptions = { RuntimeException.class })
    public void testConfigValidation() throws NumberFormatException, UnknownHostException {
        MongoConnector.createClient();
    }

    @Test(enabled = false)
    public void testConnect() throws NumberFormatException, UnknownHostException {
        MongoClient mc = MongoConnector.createClient();
        DB db = mc.getDB("lcdocs");
        DBCollection collection = db.getCollection("statements.files");
        DBCursor cursor = collection.find(new BasicDBObject("filename", Pattern.compile("/.*278174.*/")));
        while (cursor.hasNext()) {
            collection.remove(cursor.next());
        }
    }
}
