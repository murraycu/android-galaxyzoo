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

import android.test.AndroidTestCase;

import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class HttpUtilsTest extends AndroidTestCase {

    @Override
    public void setUp() throws IOException {
    }

    @Override
    public void tearDown() {
    }


    public void testGenerateAuthorizationHeader() throws IOException {
        final String header = HttpUtils.generateAuthorizationHeader("somename", "somekey123");
        assertEquals("Basic c29tZW5hbWU6c29tZWtleTEyMw==", header);
    }

    public void testGetPostDataBytes() throws IOException {
        final List<HttpUtils.NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new HttpUtils.NameValuePair("classification[subject_ids][]",
                "504f217bc499611ea60410ed"));
        nameValuePairs.add(new HttpUtils.NameValuePair("classification[annotations][0][sloan-0]",
                "a-1"));
        nameValuePairs.add(new HttpUtils.NameValuePair("classification[annotations][1][sloan-1]",
                "a-1"));
        nameValuePairs.add(new HttpUtils.NameValuePair("classification[annotations][2][sloan-2]",
                "a-1"));
        nameValuePairs.add(new HttpUtils.NameValuePair("classification[annotations][3][sloan-3]",
                "a-0"));

        final String content = HttpUtils.getPostDataBytes(nameValuePairs);
        assertEquals("classification%5Bsubject_ids%5D%5B%5D=504f217bc499611ea60410ed&classification%5Bannotations%5D%5B0%5D%5Bsloan-0%5D=a-1&classification%5Bannotations%5D%5B1%5D%5Bsloan-1%5D=a-1&classification%5Bannotations%5D%5B2%5D%5Bsloan-2%5D=a-1&classification%5Bannotations%5D%5B3%5D%5Bsloan-3%5D=a-0",
                content);
    }

}
