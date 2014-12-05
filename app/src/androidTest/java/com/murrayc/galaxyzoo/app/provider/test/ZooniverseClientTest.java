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

import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class ZooniverseClientTest extends AndroidTestCase {

    @Override
    public void setUp() throws IOException {
    }

    @Override
    public void tearDown() {
    }

    public void testMoreItems() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();

        final String strResponse = getStringFromStream(
                MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_more_items_response.json"));
        assertNotNull(strResponse);
        server.enqueue(new MockResponse().setBody(strResponse));
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        final int COUNT = 5;
        final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(COUNT);
        assertNotNull(subjects);
        assertTrue(subjects.size() == COUNT);

        final ZooniverseClient.Subject subject = subjects.get(0);
        assertNotNull(subject);
        assertNotNull(subject.getSubjectId());
        assertEquals(subject.getSubjectId(), "504e6b5dc499611ea6020689");
        assertNotNull(subject.getZooniverseId());
        assertEquals(subject.getZooniverseId(), "AGZ0002ufd");
        assertNotNull(subject.getLocationStandard());
        assertEquals(subject.getLocationStandard(),
                "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/standard/1237666273680359558.jpg");
        assertNotNull(subject.getLocationThumbnail());
        assertEquals(subject.getLocationThumbnail(),
                "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/thumbnail/1237666273680359558.jpg");
        assertNotNull(subject.getLocationInverted());
        assertEquals(subject.getLocationInverted(),
                "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/inverted/1237666273680359558.jpg");


        //Test what the server received:
        assertEquals(1, server.getRequestCount());
        final RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/groups/50251c3b516bcb6ecb000002/subjects?limit=5", request.getPath());

        server.shutdown();
    }

    public void testLoginWithSuccess() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();

        final String strResponse = getStringFromStream(
                MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_login_response_success.json"));
        assertNotNull(strResponse);
        server.enqueue(new MockResponse().setBody(strResponse));
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

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

        final byte[] contents = request.getBody();
        final String strContents = new String(contents, Utils.STRING_ENCODING);
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
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        final LoginUtils.LoginResult result = client.loginSync("testusername", "testpassword");
        assertNotNull(result);
        assertFalse(result.getSuccess());

        server.shutdown();
    }

    public void testLoginWithBadResponseContent() throws IOException {
        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody("test bad login response"));
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        final LoginUtils.LoginResult result = client.loginSync("testusername", "testpassword");
        assertNotNull(result);
        assertFalse(result.getSuccess());

        server.shutdown();
    }


    public void testMoreItemsWithBadResponseContent() throws IOException {
        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody("some nonsense to try to break things {"));
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        //Mostly we want to check that it doesn't crash on a bad HTTP response.
        final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(5);
        assertTrue((subjects == null) || (subjects.size() == 0));


        server.shutdown();
    }

    public void testMoreItemsWithBadResponseCode() throws IOException {
        final MockWebServer server = new MockWebServer();

        final MockResponse response = new MockResponse();
        response.setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        response.setBody("test nonsense failure message");
        server.enqueue(response);
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        //Mostly we want to check that it doesn't crash on a bad HTTP response.
        final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(5);
        assertTrue((subjects == null) || (subjects.size() == 0));

        server.shutdown();
    }

    public void testUploadWithSuccess() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();


        final MockResponse response = new MockResponse();
        response.setResponseCode(HttpURLConnection.HTTP_CREATED);
        response.setBody("TODO");
        server.enqueue(response);
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        //SyncAdapter.doUploadSync() adds an "interface" parameter too,
        //but we are testing a more generic API here:
        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("classification[subject_ids][]", "504e4a38c499611ea6010c6a"));
        values.add(new BasicNameValuePair("classification[favorite][]", "true"));
        values.add(new BasicNameValuePair("classification[annotations][0][sloan-0]", "a-0"));
        values.add(new BasicNameValuePair("classification[annotations][1][sloan-7]", "a-1"));
        values.add(new BasicNameValuePair("classification[annotations][2][sloan-5]", "a-0"));
        values.add(new BasicNameValuePair("classification[annotations][3][sloan-6]", "x-5"));

        final boolean result = client.uploadClassificationSync("testAuthName",
                "testAuthApiKey", values);
        assertTrue(result);

        assertEquals(1, server.getRequestCount());

        //This is really just a regression test,
        //so we notice if something changes unexpectedly:
        final RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/workflows/50251c3b516bcb6ecb000002/classifications", request.getPath());
        assertNotNull(request.getHeader("Authorization"));
        assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));

        final byte[] contents = request.getBody();
        final String strContents = new String(contents, Utils.STRING_ENCODING);
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
        server.play();

        final URL mockUrl = server.getUrl("/");
        final ZooniverseClient client = new ZooniverseClient(getContext(), mockUrl.toString());

        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("test nonsense", "12345"));

        final boolean result = client.uploadClassificationSync("testAuthName",
                "testAuthApiKey", values);
        assertFalse(result);

        server.shutdown();
    }

    private String getStringFromStream(final InputStream input) throws IOException {
        //Don't bother with try/catch because we are in a test case anyway.
        final InputStreamReader isr = new InputStreamReader(input, Utils.STRING_ENCODING);
        final BufferedReader bufferedReader =  new BufferedReader(isr);

        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

}
