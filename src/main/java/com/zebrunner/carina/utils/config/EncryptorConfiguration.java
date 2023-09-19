package com.zebrunner.carina.utils.config;

import java.util.Optional;

public class EncryptorConfiguration extends Configuration {

    public enum Parameter implements IParameter {

        CRYPTO_KEY_VALUE("crypto_key_value") {
            @Override
            public boolean hidden() {
                return true;
            }
        },

        CRYPTO_PATTERN("crypto_pattern"),

        CRYPTO_WRAPPER("crypto_wrapper"),

        CRYPTO_ALGORITHM("crypto_algorithm") {
            @Override
            public boolean hidden() {
                return true;
            }
        };

        private final String name;

        Parameter(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    @Override
    public String toString() {
        Optional<String> asString = asString(Parameter.values());
        return asString.map(s -> "\n=========== Encryptor configuration ===========\n" +
                s).orElse("");
    }
}
