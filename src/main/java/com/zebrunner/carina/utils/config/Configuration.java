package com.zebrunner.carina.utils.config;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.encryptor.EncryptorUtils;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.reflect.ConstructorUtils;

public class Configuration {
    private static final String REQUIRE_VALUE_ERROR_MESSAGE = "Getting the value of parameter '%s' as required failed: the value is missing.";

    private static final MutableObject<IEnvArgResolver> ENV_ARG_RESOLVER = new MutableObject<>(new DefaultEnvArgResolver());

    public enum Parameter implements IParameter {

        /**
         * Environment specific configuration feature
         * 
         * @see <a href="https://zebrunner.github.io/carina/configuration/#environment-specific-configuration">documentation</a>
         */
        ENV("env"),

        /**
         * Path to a folder where the testing report(s) will be saved
         */
        PROJECT_REPORT_DIRECTORY("project_report_directory");

        private final String name;

        Parameter(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    private static final ConfigurationOption[] DEFAULT_CONFIG_OPTIONS = Set.of(StandardConfigurationOption.ENVIRONMENT,
            StandardConfigurationOption.GLOBAL)
            .toArray(new ConfigurationOption[0]);

    public static Optional<String> get(IParameter parameter) {
        return get(parameter, DEFAULT_CONFIG_OPTIONS);
    }

    public static Optional<String> get(IParameter parameter, ConfigurationOption... options) {
        return get(parameter.getKey(), options);
    }

    public static Optional<String> get(String parameter) {
        return get(parameter, DEFAULT_CONFIG_OPTIONS);
    }

    /**
     * Get configuration value
     *
     * @param parameter parameter key.
     * @return the {@link Optional} of parameter value if it is found by key, or {@link Optional#empty()} if not
     */
    public static Optional<String> get(String parameter, ConfigurationOption... options) {
        String value = null;

        if (!ArrayUtils.contains(options, StandardConfigurationOption.GLOBAL)
                && !ArrayUtils.contains(options, StandardConfigurationOption.ENVIRONMENT)) {
            options = ArrayUtils.addAll(options, DEFAULT_CONFIG_OPTIONS);
        }
        if (ArrayUtils.contains(options, StandardConfigurationOption.GLOBAL)) {
            Optional<String> globalValue = getGlobalParameter(parameter);
            if (globalValue.isPresent()) {
                value = globalValue.get();
            }
        }

        if (ArrayUtils.contains(options, StandardConfigurationOption.ENVIRONMENT) && !(Parameter.ENV.getKey().equals(parameter))) {
            Optional<String> envValue = getEnvironmentParameter(parameter);
            if (envValue.isPresent()) {
                value = envValue.get();
            }
        }

        if (value == null) {
            return Optional.empty();
        }

        if (ArrayUtils.contains(options, StandardConfigurationOption.DECRYPT)) {
            value = EncryptorUtils.decrypt(value);
        }
        return Optional.of(value);
    }

    protected static Optional<String> getGlobalParameter(String parameter) {
        String value = R.CONFIG.get(parameter);
        if (value == null || "NULL".equalsIgnoreCase(value) || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    protected static Optional<String> getEnvironmentParameter(String parameter) {
        String environment = R.CONFIG.get(Parameter.ENV.getKey());
        if (environment == null || "NULL".equalsIgnoreCase(environment) || environment.isEmpty()) {
            return Optional.empty();
        }

        String value = ENV_ARG_RESOLVER.getValue().get(environment, parameter);
        if (value == null || "NULL".equalsIgnoreCase(value) || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static <T> Optional<T> get(IParameter parameter, Class<T> clazz) {
        return get(parameter.getKey(), clazz, DEFAULT_CONFIG_OPTIONS);
    }

    public static <T> Optional<T> get(IParameter parameter, Class<T> clazz, ConfigurationOption... options) {
        return get(parameter.getKey(), clazz, options);
    }

    public static <T> Optional<T> get(String parameter, Class<T> clazz) {
        return get(parameter, clazz, DEFAULT_CONFIG_OPTIONS);
    }

    public static <T> Optional<T> get(String parameter, Class<T> clazz, ConfigurationOption... options) {
        Optional<String> optionalValue = get(parameter, options);
        if (optionalValue.isEmpty()) {
            return Optional.empty();
        }
        String stringValue = optionalValue.get().trim();
        Object value;
        if (clazz == String.class) {
            value = stringValue;
        } else if (clazz == Integer.class) {
            value = Integer.valueOf(stringValue);
        } else if (clazz == Long.class) {
            value = Long.valueOf(stringValue);
        } else if (clazz == Double.class) {
            value = Double.valueOf(stringValue);
        } else if (clazz == Boolean.class) {
            value = Boolean.valueOf(stringValue);
        } else if (clazz == Short.class) {
            value = Short.valueOf(stringValue);
        } else if (clazz == Byte.class) {
            value = Byte.valueOf(stringValue);
        } else {
            throw new IllegalArgumentException(
                    String.format("Cannot get parameter value with specific type. Class '%s' is unsupported by method.", clazz));
        }
        return Optional.of(clazz.cast(value));
    }

    public static String getRequired(IParameter parameter) {
        return getRequired(parameter, DEFAULT_CONFIG_OPTIONS);
    }

    public static String getRequired(IParameter parameter, ConfigurationOption... options) {
        return get(parameter, options)
                .orElseThrow(() -> new InvalidConfigurationException(
                        String.format(REQUIRE_VALUE_ERROR_MESSAGE, parameter.getKey())));
    }

    public static String getRequired(String parameter) {
        return getRequired(parameter, DEFAULT_CONFIG_OPTIONS);
    }

    public static String getRequired(String parameter, ConfigurationOption... options) {
        return get(parameter, options)
                .orElseThrow(() -> new InvalidConfigurationException(
                        String.format(REQUIRE_VALUE_ERROR_MESSAGE, parameter)));
    }

    public static <T> T getRequired(IParameter parameter, Class<T> clazz) {
        return getRequired(parameter, clazz, DEFAULT_CONFIG_OPTIONS);
    }

    public static <T> T getRequired(IParameter parameter, Class<T> clazz, ConfigurationOption... options) {
        return get(parameter.getKey(), clazz, options)
                .orElseThrow(() -> new InvalidConfigurationException(
                        String.format(REQUIRE_VALUE_ERROR_MESSAGE, parameter)));
    }

    public static <T> T getRequired(String parameter, Class<T> clazz) {
        return getRequired(parameter, clazz, DEFAULT_CONFIG_OPTIONS);
    }

    public static <T> T getRequired(String parameter, Class<T> clazz, ConfigurationOption... options) {
        return get(parameter, clazz, options)
                .orElseThrow(() -> new InvalidConfigurationException(
                        String.format(REQUIRE_VALUE_ERROR_MESSAGE, parameter)));
    }

    public static void setEnvironmentArgumentResolver(Class<? extends IEnvArgResolver> clazz) {
        try {
            ENV_ARG_RESOLVER.setValue(ConstructorUtils.invokeConstructor(Objects.requireNonNull(clazz)));
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            ExceptionUtils.rethrow(e);
        }
    }

    protected Optional<String> asString(IParameter[] parameters) {
        String lineFormat = "%s=%s%n";
        StringBuilder asString = new StringBuilder();
        for (IParameter param : parameters) {
            String str = StringUtils.EMPTY;
            Optional<String> global = get(param);
            if (global.isPresent()) {
                if (R.CONFIG.isOverwritten(param.getKey())) {
                    str = param.hidden() ? "*****" : global.get();
                }
            }
            if (!str.isEmpty()) {
                asString.append(String.format(lineFormat, param.getKey(), str));
            }
        }

        if (asString.length() <= 0) {
            return Optional.empty();
        }
        return Optional.of(asString.toString());
    }

    @Override
    public String toString() {
        Optional<String> asString = asString(Parameter.values());
        if (asString.isEmpty()) {
            return "";
        }
        return "\n============= Basic configuration =============\n" +
                asString.get();
    }
}
