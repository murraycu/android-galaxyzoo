/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-glom
 *
 * android-glom is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-glom is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-glom.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.R;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.IOException;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class LoginUtilsTest extends AndroidTestCase {


    @Override
    public void setUp() throws IOException {
    }

    @Override
    public void tearDown() {
    }

    public void testEncryptNull() {

        assertNull(LoginUtils.encryptString(getContext(), null));
    }

    public void testDecryptNull() {
        assertNull(LoginUtils.decryptString(getContext(), null, null));
    }

    /*
    public void testSecretKeyRetrieval() {
        final Context context = getContext();
        LoginUtils.wipeEncryptionKey(context);
        final SecretKey generated = LoginUtils.generateEncryptionKey();
        LoginUtils.saveEncryptionKey(context, generated);

        final SecretKey retrieved = LoginUtils.getEncryptionKey(context);

        assertEquals(generated, retrieved);
    }
    */

    public void testSaveAuth() {
        final Context context = getContext();
        final String originalAuthName = "some auth name";
        final String originalAuthApiKey = "some auth api key";

        LoginUtils.saveAuthToPreferences(context, originalAuthName, originalAuthApiKey);

        final LoginUtils.LoginDetails details = LoginUtils.getPrefsAuth(context);
        assertNotNull(details);
        assertNotNull(details.authName);
        assertEquals(originalAuthName, details.authName);
        assertNotNull(details.authApiKey);
        assertEquals(originalAuthApiKey, details.authApiKey);
    }


    public void testGetAuthWithBrokenIvPreferences() {
        final Context context = getContext();
        final String originalAuthName = "some auth name";
        final String originalAuthApiKey = "some auth api key";

        LoginUtils.saveAuthToPreferences(context, originalAuthName, originalAuthApiKey);

        //Damage the preferences by removing the initialization vectors.
        //We are testing that it fails without crashing.
        Utils.setBytesPref(context, R.string.pref_key_auth_name_initialization_vector,
                null);
        Utils.setBytesPref(context, R.string.pref_key_auth_api_key,
                null);

        final LoginUtils.LoginDetails details = LoginUtils.getPrefsAuth(context);
        if (details != null) {
            assertTrue(!TextUtils.equals(originalAuthName, details.authName));
            assertTrue(!TextUtils.equals(originalAuthApiKey, details.authApiKey));
        }
    }

    public void testGetAuthWithBrokenEncryptionKeyPreferences() {
        final Context context = getContext();
        final String originalAuthName = "some auth name";
        final String originalAuthApiKey = "some auth api key";

        LoginUtils.saveAuthToPreferences(context, originalAuthName, originalAuthApiKey);

        //Damage the preferences by removing the encryption key.
        //We are testing that it fails without crashing.
        Utils.setBytesPref(context, R.string.pref_key_auth_encryption_key, null);

        final LoginUtils.LoginDetails details = LoginUtils.getPrefsAuth(context);
        if (details != null) {
            assertTrue(!TextUtils.equals(originalAuthName, details.authName));
            assertTrue(!TextUtils.equals(originalAuthApiKey, details.authApiKey));
        }
    }


    public void testEncryptDecrypt() {
        final String original = "Some original text";

        final LoginUtils.EncryptionResult encrypted = LoginUtils.encryptString(getContext(), original);
        assertFalse(TextUtils.equals(original, encrypted.encryptedString));
        final String decrypted = LoginUtils.decryptString(getContext(), encrypted.encryptedString, encrypted.iv);
        assertEquals(original, decrypted);
    }
}
