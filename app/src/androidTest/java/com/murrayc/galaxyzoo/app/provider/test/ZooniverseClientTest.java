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
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    public void testMoreItems() throws IOException {
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
        assertNotNull(subject.mId);
        assertEquals(subject.mId, "504e6b5dc499611ea6020689");
        assertNotNull(subject.mZooniverseId);
        assertEquals(subject.mZooniverseId, "AGZ0002ufd");
        assertNotNull(subject.mLocationStandard);
        assertEquals(subject.mLocationStandard, "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/standard/1237666273680359558.jpg");
        assertNotNull(subject.mLocationThumbnail);
        assertEquals(subject.mLocationThumbnail, "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/thumbnail/1237666273680359558.jpg");
        assertNotNull(subject.mLocationInverted);
        assertEquals(subject.mLocationInverted, "http://www.galaxyzoo.org.s3.amazonaws.com/subjects/inverted/1237666273680359558.jpg");

        server.shutdown();
    }

    public void testLoginWithSuccess() throws IOException {
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

    private String getStringFromStream(final InputStream input) throws IOException {
        //Don't bother with try/catch because we are in a test case anyway.
        final InputStreamReader isr = new InputStreamReader(input, "UTF-8");
        final BufferedReader bufferedReader =  new BufferedReader(isr);

        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

}
