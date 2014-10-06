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

import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.DecisionTree;
import com.murrayc.galaxyzoo.app.LoginUtils;

import java.io.IOException;
import java.io.InputStream;

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
        assertNull(LoginUtils.decryptString(getContext(), null));
    }

    public void testEncryptDecrypt() {
        final String original = "Some original text";
        final String encrypted = LoginUtils.encryptString(getContext(), original);
        assertFalse(TextUtils.equals(original, encrypted));
        final String decrypted = LoginUtils.decryptString(getContext(), encrypted);
        assertEquals(original, decrypted);
    }




    }
