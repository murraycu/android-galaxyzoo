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

package com.murrayc.galaxyzoo.app.provider.test;

import android.support.test.runner.AndroidJUnit4;

import com.murrayc.galaxyzoo.app.LoginUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Simple test to ensure that the generated bindings are working.
 */
@RunWith(AndroidJUnit4.class)
public class LoginResponseJsonParserTest {
    @Test
    public void testParseSuccess() throws IOException {
        final InputStream inputStream = LoginResponseJsonParserTest.class.getClassLoader().getResourceAsStream("test_login_response_success.json");
        assertNotNull(inputStream);
        final LoginUtils.LoginResult result = LoginUtils.parseLoginResponseContent(inputStream);
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(result.getName(), "testuser");
        assertEquals(result.getApiKey(), "testapikey");
    }

}
