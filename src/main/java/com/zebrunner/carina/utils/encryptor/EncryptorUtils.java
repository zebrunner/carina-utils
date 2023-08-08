package com.zebrunner.carina.utils.encryptor;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.zebrunner.carina.crypto.Algorithm;
import com.zebrunner.carina.crypto.SecretKeyManager;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.EncryptorConfiguration;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;

/**
 * <b>Beta. Could be changed in future releases</b>
 */
public final class EncryptorUtils {
    private static final Pattern DECRYPT_PATTERN = Pattern.compile(Configuration.get(EncryptorConfiguration.Parameter.CRYPTO_PATTERN).map(p -> {
        validatePattern(p);
        return p;
    }).orElseThrow(() -> new InvalidConfigurationException("'crypto_pattern' parameter could not be null.")));
    private static final Pattern ENCRYPT_PATTERN = Pattern.compile("^(?<data>.+?)$");
    private static final String ENCRYPT_WRAPPER = Configuration.getRequired(EncryptorConfiguration.Parameter.CRYPTO_WRAPPER);
    private static final String DECRYPT_WRAPPER = "%s";
    private static final Algorithm ALGORITHM = Algorithm.find(Configuration.getRequired(EncryptorConfiguration.Parameter.CRYPTO_ALGORITHM));

    private static final ConcurrentInitializer<Key> KEY_LAZY_INITIALIZER = new LazyInitializer<Key>() {
        @Override
        protected Key initialize() throws ConcurrentException {
            return SecretKeyManager.getKeyFromString(ALGORITHM, Configuration.getRequired(EncryptorConfiguration.Parameter.CRYPTO_KEY_VALUE));
        }
    };

    private static final ThreadLocal<Cipher> CRYPTO_TOOL_ENCRYPT = ThreadLocal.withInitial(() -> {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM.getName());
            cipher.init(Cipher.ENCRYPT_MODE, KEY_LAZY_INITIALIZER.get());
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | ConcurrentException e) {
            throw new RuntimeException("Cannot create cipher instance.");
        }
    });

    private static final ThreadLocal<Cipher> CRYPTO_TOOL_DECRYPT = ThreadLocal.withInitial(() -> {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM.getName());
            cipher.init(Cipher.DECRYPT_MODE, KEY_LAZY_INITIALIZER.get());
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | ConcurrentException e) {
            throw new RuntimeException("Cannot create cipher instance.");
        }
    });

    private EncryptorUtils() {
        // hide
    }

    public static String encrypt(String str) {
        return encrypt(str, ENCRYPT_PATTERN, ENCRYPT_WRAPPER);
    }

    public static String encrypt(String str, Pattern pattern) {
        return encrypt(str, pattern, ENCRYPT_WRAPPER);
    }

    public static String decrypt(String str) {
        return decrypt(str, DECRYPT_PATTERN, DECRYPT_WRAPPER);
    }

    public static String decrypt(String str, Pattern pattern) {
        return decrypt(str, pattern, DECRYPT_WRAPPER);
    }

    public static String encrypt(String str, Pattern pattern, String wrapper) {
        Matcher matcher = pattern.matcher(str);

        while (matcher.find()) {
            String dataToEncrypt = getDataGroup(matcher.group(), pattern);
            if (dataToEncrypt.isEmpty()) {
                continue;
            }
            str = StringUtils.replace(str, matcher.group(), String.format(wrapper, encryptSingleData(dataToEncrypt)));
        }
        return str;
    }

    public static String decrypt(String str, Pattern pattern, String wrapper) {
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String dataToDecrypt = getDataGroup(matcher.group(), pattern);
            if (dataToDecrypt.isEmpty()) {
                continue;
            }
            str = StringUtils.replace(str, matcher.group(), String.format(wrapper, decryptSingleData(dataToDecrypt)));
        }
        return str;
    }

    public static boolean hasMatch(String str, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    /**
     * <b>for internal usage only</b>
     */
    public static void clear() {
        CRYPTO_TOOL_DECRYPT.remove();
        CRYPTO_TOOL_ENCRYPT.remove();
    }

    private static String encryptSingleData(String str) {
        try {
            return new String(Base64.encodeBase64(CRYPTO_TOOL_ENCRYPT.get().doFinal(str.getBytes())));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Cannot encrypt.");
        }
    }

    private static String decryptSingleData(String str) {
        try {
            return new String(CRYPTO_TOOL_DECRYPT.get().doFinal(Base64.decodeBase64(str.getBytes())));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getDataGroup(String str, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        return matcher.find() ? matcher.group("data") : StringUtils.EMPTY;
    }

    private static void validatePattern(String pattern) {
        // Check is pattern contains data group
        if (!pattern.contains("(?<data>")) {
            throw new IllegalArgumentException("There are no data group in pattern: " + pattern);
        }
    }
}
