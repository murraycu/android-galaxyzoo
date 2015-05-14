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
import android.util.MalformedJsonException;

import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okio.Buffer;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class ZooniverseClientTest extends AndroidTestCase {

    public static final String TEST_GROUP_ID = "551453e12f0eef21f2000001";

    @Override
    public void setUp() throws IOException {
    }

    @Override
    public void tearDown() {
    }

    public void testMoreItems() throws IOException, InterruptedException, ZooniverseClient.RequestMoreItemsException {
        final MockWebServer server = new MockWebServer();

        final String strResponse = getStringFromStream(
                MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_more_items_response.json"));
        assertNotNull(strResponse);
        server.enqueue(new MockResponse().setBody(strResponse));
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);

        final int COUNT = 5;
        final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(COUNT);
        assertNotNull(subjects);
        assertTrue(subjects.size() == COUNT);

        final ZooniverseClient.Subject subject = subjects.get(0);
        assertNotNull(subject);

        assertNotNull(subject.getSubjectId());
        assertEquals(subject.getSubjectId(), "5500684569736d5964271400");

        assertNotNull(subject.getZooniverseId());
        assertEquals(subject.getZooniverseId(), "AGZ00081ls");

        assertNotNull(subject.getGroupId());
        assertEquals(subject.getGroupId(), TEST_GROUP_ID);

        assertNotNull(subject.getLocationStandard());
        assertEquals(subject.getLocationStandard(),
                "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/standard/goods_full_n_27820_standard.jpg");
        assertNotNull(subject.getLocationThumbnail());
        assertEquals(subject.getLocationThumbnail(),
                "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/thumbnail/goods_full_n_27820_thumbnail.jpg");
        assertNotNull(subject.getLocationInverted());
        assertEquals(subject.getLocationInverted(),
                "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/inverted/goods_full_n_27820_inverted.jpg");


        //Test what the server received:
        assertEquals(1, server.getRequestCount());
        final RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());

        //ZooniverseClient uses one of several possible group IDs at random:
        //See com.murrayc.galaxyzoo.app.provider.Config
        final String possiblePath1 = "/groups/" + TEST_GROUP_ID + "/subjects?limit=5";
        final String possiblePath2 = "/groups/" + "5514521e2f0eef2012000001" + "/subjects?limit=5";
        //TODO: Can we use this?
        // assertThat(request.getPath(), anyOf(is(possiblePath1), is(possiblePath2)));
        final String path = request.getPath();
        assertTrue( path.equals(possiblePath1) || path.equals(possiblePath2));

        server.shutdown();
    }

    public void testLoginWithSuccess() throws IOException, InterruptedException, ZooniverseClient.LoginException {
        final MockWebServer server = new MockWebServer();

        final String strResponse = getStringFromStream(
                MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_login_response_success.json"));
        assertNotNull(strResponse);
        server.enqueue(new MockResponse().setBody(strResponse));
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);

        final LoginUtils.LoginResult result = client.loginSync("testusername", "testpassword");
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(result.getApiKey(), "testapikey");

        //Test what the server received:
        assertEquals(1, server.getRequestCount());
        final RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/login", request.getPath());
        assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));

        final Buffer contents = request.getBody();
        final String strContents = contents.readUtf8();
        assertEquals("username=testusername&password=testpassword",
                strContents);

        server.shutdown();
    }

    public void testLoginWithFailure() throws IOException {
        final MockWebServer server = new MockWebServer();


        //On failure, the server's response code is HTTP_OK,
        //but it has a "success: false" parameter.
        final MockResponse response = new MockResponse();
        response.setResponseCode(HttpURLConnection.HTTP_OK);
        response.setBody("test nonsense failure message");
        server.enqueue(response);
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);


        try {
            final LoginUtils.LoginResult result = client.loginSync("testusername", "testpassword");
            assertNotNull(result);
            assertFalse(result.getSuccess());
        } catch (final ZooniverseClient.LoginException e) {
            assertTrue(e.getCause() instanceof MalformedJsonException);
        }



        server.shutdown();
    }

    public void testLoginWithBadResponseContent() throws IOException {
        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody("test bad login response"));
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);


        try {
            final LoginUtils.LoginResult result = client.loginSync("testusername", "testpassword");
            assertNotNull(result);
            assertFalse(result.getSuccess());
        } catch (final ZooniverseClient.LoginException e) {
            assertTrue(e.getCause() instanceof IOException);
        }

        server.shutdown();
    }


    public void testMoreItemsWithBadResponseContent() throws IOException {
        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody("some nonsense to try to break things {"));
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);

        //Mostly we want to check that it doesn't crash on a bad HTTP response.
        try {
            final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(5);
            assertTrue((subjects == null) || (subjects.isEmpty()));
        } catch (final ZooniverseClient.RequestMoreItemsException e) {
            assertTrue(e.getCause() instanceof IOException);
        }


        server.shutdown();
    }

    public void testMoreItemsWithBadResponseCode() throws IOException {
        final MockWebServer server = new MockWebServer();

        final MockResponse response = new MockResponse();
        response.setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        response.setBody("test nonsense failure message");
        server.enqueue(response);
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);

        //Mostly we want to check that it doesn't crash on a bad HTTP response.

        try {
            final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(5);
            assertTrue((subjects == null) || (subjects.isEmpty()));
        } catch (final ZooniverseClient.RequestMoreItemsException e) {
            assertTrue(e.getCause() instanceof ExecutionException);
        }

        server.shutdown();
    }

    public void testUploadWithSuccess() throws IOException, InterruptedException, ZooniverseClient.UploadException {
        final MockWebServer server = new MockWebServer();


        final MockResponse response = new MockResponse();
        response.setResponseCode(HttpURLConnection.HTTP_CREATED);
        response.setBody("TODO");
        server.enqueue(response);
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);

        //SyncAdapter.doUploadSync() adds an "interface" parameter too,
        //but we are testing a more generic API here:
        final List<ZooniverseClient.NameValuePair> values = new ArrayList<>();
        values.add(new ZooniverseClient.NameValuePair("classification[subject_ids][]", "504e4a38c499611ea6010c6a"));
        values.add(new ZooniverseClient.NameValuePair("classification[favorite][]", "true"));
        values.add(new ZooniverseClient.NameValuePair("classification[annotations][0][sloan-0]", "a-0"));
        values.add(new ZooniverseClient.NameValuePair("classification[annotations][1][sloan-7]", "a-1"));
        values.add(new ZooniverseClient.NameValuePair("classification[annotations][2][sloan-5]", "a-0"));
        values.add(new ZooniverseClient.NameValuePair("classification[annotations][3][sloan-6]", "x-5"));

        final boolean result = client.uploadClassificationSync("testAuthName",
                "testAuthApiKey", TEST_GROUP_ID, values);
        assertTrue(result);

        assertEquals(1, server.getRequestCount());

        //This is really just a regression test,
        //so we notice if something changes unexpectedly:
        final RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/workflows/" + TEST_GROUP_ID + "/classifications", request.getPath());
        assertNotNull(request.getHeader("Authorization"));
        assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));

        final Buffer contents = request.getBody();
        final String strContents = contents.readUtf8();
        assertEquals("classification%5Bsubject_ids%5D%5B%5D=504e4a38c499611ea6010c6a&classification%5Bfavorite%5D%5B%5D=true&classification%5Bannotations%5D%5B0%5D%5Bsloan-0%5D=a-0&classification%5Bannotations%5D%5B1%5D%5Bsloan-7%5D=a-1&classification%5Bannotations%5D%5B2%5D%5Bsloan-5%5D=a-0&classification%5Bannotations%5D%5B3%5D%5Bsloan-6%5D=x-5",
                strContents);

        server.shutdown();
    }

    public void testUploadWithFailure() throws IOException {
        final MockWebServer server = new MockWebServer();

        final MockResponse response = new MockResponse();
        response.setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED);
        response.setBody("test nonsense failure message");
        server.enqueue(response);
        server.start();

        final ZooniverseClient client = createZooniverseClient(server);

        final List<ZooniverseClient.NameValuePair> values = new ArrayList<>();
        values.add(new ZooniverseClient.NameValuePair("test nonsense", "12345"));

        try {
            final boolean result = client.uploadClassificationSync("testAuthName",
                    "testAuthApiKey", TEST_GROUP_ID, values);
            assertFalse(result);
        } catch (final ZooniverseClient.UploadException e) {
            //This is (at least with okhttp.mockwebserver) a normal
            //event if the upload was refused via an error response code.
            assertTrue(e.getCause() instanceof IOException);
        }

        server.shutdown();
    }

    private ZooniverseClient createZooniverseClient(final MockWebServer server) {
        final URL mockUrl = server.getUrl("/");
        return new ZooniverseClient(getContext(), mockUrl.toString());
    }

    private static String getStringFromStream(final InputStream input) throws IOException {
        //Don't bother with try/catch because we are in a test case anyway.
        final InputStreamReader isr = new InputStreamReader(input, Utils.STRING_ENCODING);
        final BufferedReader bufferedReader =  new BufferedReader(isr);

        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        bufferedReader.close();
        isr.close();

        return sb.toString();
    }

}
