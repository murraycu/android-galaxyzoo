package com.murrayc.galaxyzoo.app.provider;

import android.content.Context;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by murrayc on 8/25/14.
 */
public class HttpUtils {

    private static final String HTTP_REQUEST_HEADER_PARAM_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_MURRAYC = "murrayc.com-android-galaxyzoo";

    public static String getStringFromInputStream(final InputStream content) throws IOException {
        final InputStreamReader inputReader = new InputStreamReader(content);
        final BufferedReader reader = new BufferedReader(inputReader);

        final StringBuilder builder = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    /**
     * Callers should use throwIfNoNetwork() before calling this.
     */
    static InputStream httpGetRequest(final String strUri) {
        final HttpURLConnection conn = openConnection(strUri);
        if (conn == null) {
            return null;
        }

        // This is the default: conn.setRequestMethod("GET");

        //We don't use Java 7's try-with-resources,
        //because we want to return the InputStream, but try-with-resources would
        //presumably close it as we returned it. TODO: Is this true?
        InputStream in = null;
        try {
            //Calling getInputStream() causes the request to actually be sent.
            in = conn.getInputStream();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.error("httpGetRequest(): response code: " + conn.getResponseCode());
                in.close();
                return null;
            }

            //HTTPUrlConnection seems to (request and) handle gzip encoding automatically,
            //so we don't need to do it here:
            /*
            //Ungzip it if necessary:
            //For instance, HTML and CSS files may often be gzipped.
            final Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                in = new GZIPInputStream(in);
            }
            */

            return in;
        } catch (final IOException e) {
            Log.error("httpGetRequest(): exception during HTTP connection", e);
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e1) {
                Log.error("httpGetRequest(): cannot close InputStream.", e);
            }
            return null;
        }
    }

    static void throwIfNoNetwork(final Context context) {
        if (!Utils.getNetworkIsConnected(context)) {
            //Throw an exception so the caller knows.
            throw new ItemsContentProvider.NoNetworkException();
        }
    }

    static HttpURLConnection openConnection(final String strURL) {
        final URL url;
        try {
            url = new URL(strURL);
        } catch (MalformedURLException e) {
            Log.error("openConnection(): exception while parsing URL", e);
            return null;
        }


        final HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
            setConnectionUserAgent(conn);
        } catch (final IOException e) {
            Log.error("openConnection(): exception during HTTP connection", e);

            return null;
        }

        return conn;
    }

    public static boolean cacheUriToFileSync(final String uriFileToCache, final String cacheFileUri) {
        final InputStream in = HttpUtils.httpGetRequest(uriFileToCache);
        if (in == null) {
            return false;
        }

        final boolean result = ItemsContentProvider.parseQueryResponseContent(in, cacheFileUri);
        try {
            in.close();
        } catch (IOException e) {
            Log.error("cacheUriToFileSync(): Can't close input stream", e);
        }

        return result;
    }

    public static long getLatestLastModified(final String[] urls) {
        long latest = 0;
        for (final String url : urls) {
            final long lastMod = getUriLastModified(url);
            if (lastMod > latest) {
                latest = lastMod;
            }
        }

        return latest;
    }

    private static long getUriLastModified(final String strUrl) {
        final URL url;
        try {
            url = new URL(strUrl);
        } catch (final MalformedURLException e) {
            Log.error("getUriLastModified(): can't instantiate URL", e);
            return 0;
        }

        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con;
        try {
            //Note: This seems to succeed even if there is no internet connection,
            //for instance if the device is in airplane mode.
            //getLastModified() then returns 0;
            con = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            Log.error("getUriLastModified(): can't openConnection", e);
            return 0;
        }

        final long result = con.getLastModified();
        con.disconnect(); //Otherwise StrictMode says (with an exception) that we leak.
        return result;
    }

    private static void setConnectionUserAgent(final HttpURLConnection connection) {
        connection.setRequestProperty(HTTP_REQUEST_HEADER_PARAM_USER_AGENT, USER_AGENT_MURRAYC);
    }

}
