package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by murrayc on 10/6/14.
 */
public class LoginUtils {

    public static final String ENCRYPTION_KEY_ALGORITHM = "AES";
    public static final String ENCRYPTION_CIPHER_TRANSFORMATION = ENCRYPTION_KEY_ALGORITHM + "/CBC/PKCS5Padding";
    public static final String STRING_ENCODING = "UTF-8";
    //public static final String ENCRYPTION_CIPHER_TRANSFORMATION = ENCRYPTION_KEY_ALGORITHM + "/GCM/NoPadding";


    //TODO: Ask the provider instead of using this hack which uses too much internal knowledge.
    public static boolean getLoggedIn(final Context context) {
        final LoginDetails loginDetails = getPrefsAuth(context);
        return (loginDetails != null) && !(TextUtils.isEmpty(loginDetails.authApiKey));
    }

    public static void saveAuthToPreferences(final Context context, final String name, final String apiKey) {
        final SharedPreferences prefs = Utils.getPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();

        //We store both the encrypted strings and the initialization vectors that were used
        //(generated) when we did that encryption:
        final EncryptionResult encryptedAuthName = encryptString(context, name);
        editor.putString(context.getString(R.string.pref_key_auth_name),
                encryptedAuthName.encryptedString);
        setBytesPref(context, R.string.pref_key_auth_name_initialization_vector,
                encryptedAuthName.iv);

        final EncryptionResult encryptedAuthApiKey = encryptString(context, apiKey);
        editor.putString(context.getString(R.string.pref_key_auth_api_key),
                encryptedAuthApiKey.encryptedString);
        setBytesPref(context, R.string.pref_key_auth_api_key_initialization_vector,
                encryptedAuthApiKey.iv);

        editor.apply();
    }

    public static class LoginDetails {
        public String authName = null;
        public String authApiKey = null;
    }

    public static LoginDetails getPrefsAuth(final Context context) {
        final LoginDetails result = new LoginDetails();
        final SharedPreferences prefs = Utils.getPreferences(context);

        //Get both the encrypted strings and the initialization vectors that were used (generated)
        //when encrypting the original strings:
        final String encryptedAuthName = prefs.getString(context.getString(R.string.pref_key_auth_name), null);
        final byte[] authNameIv = getBytesPref(context, R.string.pref_key_auth_name_initialization_vector);

        final String encryptedAuthApiKey = prefs.getString(context.getString(R.string.pref_key_auth_api_key), null);
        final byte[] authApiKeyIv = getBytesPref(context, R.string.pref_key_auth_api_key_initialization_vector);

        boolean resetAuthName = false;
        boolean resetAuthApiKey = false;

        if (!TextUtils.isEmpty(encryptedAuthName)) {
            result.authName = decryptString(context, encryptedAuthName, authNameIv);
            if(TextUtils.isEmpty(result.authName)) {
                resetAuthName = true;
            }
        }

        if (!TextUtils.isEmpty(encryptedAuthApiKey)) {
            result.authApiKey = decryptString(context, encryptedAuthApiKey, authApiKeyIv);
            if(TextUtils.isEmpty(result.authApiKey)) {
                resetAuthApiKey = true;
            }
        }

        if (resetAuthName || resetAuthApiKey) {
            //We couldn't decrypt these values so discard them,
            //keeping any that were OK:
            final String authName = resetAuthName ? null : result.authName;
            final String authApiKey = resetAuthApiKey ? null : result.authApiKey;
            saveAuthToPreferences(context, authName, authApiKey);
        }

        return result;
    }

    public static class EncryptionResult {
        public String encryptedString;
        public byte[] iv;
    };

    public static EncryptionResult encryptString(final Context context, final String input) {
        //Don't bother trying to encrypt null or an empty string:
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        final Cipher cipher = getCipher(context, Cipher.ENCRYPT_MODE,
                null /* auto-generate an initialization vector */);
        if (cipher == null) {
            return null;
        }

        //This should be the reverse of decryptString().

        //Get the bytes:
        byte[] inputAsBytes = null;
        try {
            inputAsBytes = input.getBytes("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            Log.error("encryptString(): String.getBytes() failed", e);
            return null;
        }

        //Encrypt the bytes:
        byte[] inputEncryptedAsBytes = null;
        try {
            inputEncryptedAsBytes = cipher.doFinal(inputAsBytes);
        } catch (final IllegalBlockSizeException e) {
            Log.error("encryptString(): Cipher.doFinal() failed", e);
            return null;
        } catch (final BadPaddingException e) {
            Log.error("encryptString(): Cipher.doFinal() failed", e);
            return null;
        }

        //Convert the encrypted bytes to Base64 so it can go into a String:
        final byte[] inputEncryptedBase64 = Base64.encode(inputEncryptedAsBytes, Base64.DEFAULT);

        final EncryptionResult result = new EncryptionResult();

        //Put it in a String:
        try {
            result.encryptedString = new String(inputEncryptedBase64, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            Log.error("encryptString(): new String() failed", e);
            return null;
        }

        //We store the initialization vector too
        //(generated appropriately by the Cipher,
        //though we could have supplied one instead via Cipher.init()),
        //because we need to provide this when decrypting later.
        result.iv = cipher.getIV();

        return result;
    }

    public static String decryptString(final Context context, final String input, final byte[] iv) {
        if (input == null) {
            return null;
        }

        //We need the initialization vector that was used when encrypting.
        if (iv == null) {
            return null;
        }

        //Don't bother trying to decrypt null or an empty string:
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        final Cipher cipher = getCipher(context, Cipher.DECRYPT_MODE, iv);
        if (cipher == null) {
            return null;
        }

        //This should be the reverse of encryptString().

        //Get the bytes:
        byte[] inputAsBytes = null;
        try {
            inputAsBytes = input.getBytes(STRING_ENCODING);
        } catch (UnsupportedEncodingException e) {
            Log.error("encryptString(): new String() failed", e);
            return null;
        }

        //Undo the Base64 encoding:
        final byte[] inputUnBase64ed = Base64.decode(inputAsBytes, Base64.DEFAULT);

        //Decrypt the bytes:
        byte[] inputDecryptedAsBytes = null;
        try {
            inputDecryptedAsBytes = cipher.doFinal(inputUnBase64ed);
        } catch (final IllegalBlockSizeException e) {
            Log.error("decryptString(): Cipher.doFinal() failed", e);
            return null;
        } catch (final BadPaddingException e) {
            Log.error("decryptString(): Cipher.doFinal() failed", e);
            return null;
        }

        //Convert it to a string.
        try {
            return new String(inputDecryptedAsBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.error("decryptString(): new String() failed", e);
            return null;
        }
    }

    private static Cipher getCipher(final Context context, int opmode, byte[] iv) {
        final SecretKey encryptionKey = getEncryptionKey(context);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(ENCRYPTION_CIPHER_TRANSFORMATION);
        } catch (final NoSuchAlgorithmException e) {
            Log.error("getCipher(): Cipher.getInstance() failed", e);
            return null;
        } catch (NoSuchPaddingException e) {
            Log.error("getCipher(): Cipher.getInstance() failed", e);
            return null;
        }

        try {
            //When using "AES/CBC/PKCS5Padding", but not when using "AES",
            //the Cipher generates and uses an initialization vector.
            //When decrypting we need to use the same one the was used when encrypting.
            if (iv == null) {
                //We are probably encrypting
                //and letting the Cipher generate an initialization vector.
                cipher.init(opmode, encryptionKey);
            } else {
                final IvParameterSpec ivParamSpec = new IvParameterSpec(iv);
                cipher.init(opmode, encryptionKey, ivParamSpec);
            }
        } catch (final InvalidKeyException e) {
            Log.error("getCipher(): Cipher.init() failed", e);
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            Log.error("getCipher(): Cipher.init() failed", e);
            return null;
        }

        return cipher;
    }

    private static byte[] getBytesPref(final Context context, final int prefsKeyId) {
        final SharedPreferences prefs = Utils.getPreferences(context);
        final String asString = prefs.getString(context.getString(prefsKeyId), null);
        if (!TextUtils.isEmpty(asString)) {
            final byte[] asBytes;
            try {
                asBytes = asString.getBytes(STRING_ENCODING);
            } catch (UnsupportedEncodingException e) {
                Log.error("getEncryptionKey(): String.getBytes() failed.", e);
                return null;
            }

            return Base64.decode(asBytes, Base64.DEFAULT);
        }

        return null;
    }

    private static void setBytesPref(final Context context, int prefKeyId, byte[] bytes) {
        final byte[] asBytesBase64 = Base64.encode(bytes, Base64.DEFAULT);
        String asString = null;
        try {
            asString = new String(asBytesBase64, STRING_ENCODING);
        } catch (final UnsupportedEncodingException e) {
            Log.error("getEncryptionKey(): new String() failed.", e);
        }

        final SharedPreferences prefs = Utils.getPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(prefKeyId), asString);
        editor.apply();

    }

    /*
    public static void wipeEncryptionKey(final Context context) {
        final SharedPreferences prefs = Utils.getPreferences(context);

        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_key_auth_encryption_key), null);
        editor.apply();
    }
    */

    private static SecretKey getEncryptionKey(final Context context) {
        //Get the already-generated encryption key if any:
        final SharedPreferences prefs = Utils.getPreferences(context);
        final String keyAsString = prefs.getString(context.getString(R.string.pref_key_auth_encryption_key), null);
        if (!TextUtils.isEmpty(keyAsString)) {
            final byte[] keyAsBytes;
            try {
                keyAsBytes = keyAsString.getBytes(STRING_ENCODING);
            } catch (UnsupportedEncodingException e) {
                Log.error("getEncryptionKey(): String.getBytes() failed.", e);
                return null;
            }

            final byte[] keyAsBytesUnBase64ed = Base64.decode(keyAsBytes, Base64.DEFAULT);
            return new SecretKeySpec(keyAsBytesUnBase64ed, ENCRYPTION_KEY_ALGORITHM);
        }

        //Generate it and store it for next time:
        //This should only happen the first time the app is launched.
        final SecretKey result = generateEncryptionKey();
        saveEncryptionKey(context, result);

        return result;
    }

    private static void saveEncryptionKey(final Context context, final SecretKey encryptionKey) {
        final byte[] keyAsBytes = encryptionKey.getEncoded();
        setBytesPref(context, R.string.pref_key_auth_encryption_key, keyAsBytes);
    }

    //See http://android-developers.blogspot.co.uk/2013/02/using-cryptography-to-store-credentials.html
    private static SecretKey generateEncryptionKey() {
        // Generate a 256-bit key
        final int outputKeyLength = 256;

        // Do *not* seed secureRandom! Automatically seeded from system entropy.
        final SecureRandom secureRandom = new SecureRandom();

        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(ENCRYPTION_KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            Log.error("generateEncryptionKey(): KeyGenerator.getInstance() failed", e);
            return null;
        }

        keyGenerator.init(outputKeyLength, secureRandom);
        return keyGenerator.generateKey();
    }
}
