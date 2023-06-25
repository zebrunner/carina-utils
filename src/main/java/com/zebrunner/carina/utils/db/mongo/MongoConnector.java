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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;

/**
 * MongoConnector - factory for MongoDB client creation.
 *
 * @author Aliaksei_Khursevich
 *         <a href="mailto:hursevich@gmail.com">Aliaksei_Khursevich</a>
 * @deprecated not used
 */
@Deprecated(forRemoval = true, since = "1.0.5")
public final class MongoConnector {
    private static final Map<String, MongoClient> clients = new HashMap<>();

    private static String host = R.DATABASE.get("mongo.host");
    private static String port = R.DATABASE.get("mongo.port");
    private static String user = R.DATABASE.get("mongo.user");
    private static String password = R.DATABASE.get("mongo.password");
    private static String database = R.DATABASE.get("mongo.database");

    private MongoConnector() {
    }

    /**
     * Creates client for DB specified in properties.
     * 
     * @return MongoDB client
     * @throws NumberFormatException java.lang.NumberFormatException
     * @throws UnknownHostException java.net.UnknownHostException
     */
    public static MongoClient createClient() throws UnknownHostException {
        if (!clients.containsKey(database)) {
            validateConfig(database);
            MongoCredential credential = MongoCredential.createMongoCRCredential(user, database, password.toCharArray());
            clients.put(database, new MongoClient(new ServerAddress(host, Integer.valueOf(port)), Arrays.asList(credential)));
        }
        return clients.get(database);
    }

    /**
     * Creates client for DB specified by parameter.
     * 
     * @param database DB
     * @return MongoDB client
     * @throws NumberFormatException java.lang.NumberFormatException
     * @throws UnknownHostException java.net.UnknownHostException
     */
    public static MongoClient createClient(String database) throws UnknownHostException {
        if (!clients.containsKey(database)) {
            validateConfig(database);
            MongoCredential credential = MongoCredential.createMongoCRCredential(user, database, password.toCharArray());
            clients.put(database, new MongoClient(new ServerAddress(host, Integer.valueOf(port)), Arrays.asList(credential)));
        }
        return clients.get(database);
    }

    private static void validateConfig(String database) {
        if (StringUtils.isEmpty(host) || SpecialKeywords.MUST_OVERRIDE.equals(host)
                || StringUtils.isEmpty(port) || SpecialKeywords.MUST_OVERRIDE.equals(port)
                || StringUtils.isEmpty(user) || SpecialKeywords.MUST_OVERRIDE.equals(user)
                || StringUtils.isEmpty(password) || StringUtils.equals(SpecialKeywords.MUST_OVERRIDE, password)
                || StringUtils.isEmpty(database) || SpecialKeywords.MUST_OVERRIDE.equals(database)) {
            throw new InvalidConfigurationException("Invalid MongoDB config!");
        }
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        MongoConnector.host = host;
    }

    public static String getPort() {
        return port;
    }

    public static void setPort(String port) {
        MongoConnector.port = port;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        MongoConnector.user = user;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        MongoConnector.password = password;
    }

    public static String getDatabase() {
        return database;
    }

    public static void setDatabase(String database) {
        MongoConnector.database = database;
    }
}
