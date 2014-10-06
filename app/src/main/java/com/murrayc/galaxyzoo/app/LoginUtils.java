package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by murrayc on 10/6/14.
 */
public class LoginUtils {

    public static final String ENCRYPTION_KEY_ALGORITHM = "AES";
    public static final String ENCRYPTION_CIPHER_TRANSFORMATION = ENCRYPTION_KEY_ALGORITHM; //+ "/CBC/PKCS5Padding";
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
        editor.putString(context.getString(R.string.pref_key_auth_name),
                encryptString(context, name));
        editor.putString(context.getString(R.string.pref_key_auth_api_key),
                encryptString(context, apiKey));
        editor.apply();
    }

    public static class LoginDetails {
        public String authName = null;
        public String authApiKey = null;
    }

    public static LoginDetails getPrefsAuth(final Context context) {
        final LoginDetails result = new LoginDetails();
        final SharedPreferences prefs = Utils.getPreferences(context);

        final String encryptedAuthName = prefs.getString(context.getString(R.string.pref_key_auth_name), null);
        final String encryptedAuthApiKey = prefs.getString(context.getString(R.string.pref_key_auth_api_key), null);

        boolean resetAuthName = false;
        boolean resetAuthApiKey = false;

        if (!TextUtils.isEmpty(encryptedAuthName)) {
            result.authName = decryptString(context, encryptedAuthName);
            if(TextUtils.isEmpty(result.authName)) {
                resetAuthName = true;
            }
        }

        if (!TextUtils.isEmpty(encryptedAuthApiKey)) {
            result.authApiKey = decryptString(context, encryptedAuthApiKey);
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

    public static String encryptString(final Context context, final String input) {
        //Don't bother trying to encrypt null or an empty string:
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        final Cipher cipher = getCipher(context, Cipher.ENCRYPT_MODE);
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

        //Put it in a String:
        try {
            return new String(inputEncryptedBase64, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            Log.error("encryptString(): new String() failed", e);
            return null;
        }
    }

    public static String decryptString(final Context context, final String input) {
        //Don't bother trying to decrypt null or an empty string:
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        final Cipher cipher = getCipher(context, Cipher.DECRYPT_MODE);
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

    private static Cipher getCipher(final Context context, int opmode) {
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
            cipher.init(opmode, encryptionKey);
        } catch (final InvalidKeyException e) {
            Log.error("getCipher(): Cipher.init() failed", e);
            return null;
        }

        return cipher;
    }

    public static void wipeEncryptionKey(final Context context) {
        final SharedPreferences prefs = Utils.getPreferences(context);

        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_key_auth_encryption_key), null);
        editor.apply();
    }

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

        final byte[] keyAsBytesBase64 = Base64.encode(keyAsBytes, Base64.DEFAULT);
        String keyAsString = null;
        try {
            keyAsString = new String(keyAsBytesBase64, STRING_ENCODING);
        } catch (final UnsupportedEncodingException e) {
            Log.error("getEncryptionKey(): new String() failed.", e);
        }

        final SharedPreferences prefs = Utils.getPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_key_auth_encryption_key), keyAsString);
        editor.apply();
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
