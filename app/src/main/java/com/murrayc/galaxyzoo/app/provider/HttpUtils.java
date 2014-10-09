package com.murrayc.galaxyzoo.app.provider;

import android.content.Context;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by murrayc on 8/25/14.
 */
public class HttpUtils {

    private static final String HTTP_REQUEST_HEADER_PARAM_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_MURRAYC = "murrayc.com-android-galaxyzoo";

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
            throw new NoNetworkException();
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

        final boolean result = parseGetFileResponseContent(in, cacheFileUri);
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

    private static boolean parseGetFileResponseContent(final InputStream in, final String cacheFileUr) {
        //Write the content to the file:
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(cacheFileUr);
            // TODO: Find a way to use writeTo(), instead of looping ourselves,
            // while also having optional ungzipping?
            //response.getEntity().writeTo(fout);

            byte[] bytes = new byte[256];
            int r;
            do {
                r = in.read(bytes);
                if (r >= 0) {
                    fout.write(bytes, 0, r);
                }
            } while (r >= 0);
        } catch (final IOException e) {
            Log.error("parseGetFileResponseContent(): Exception while writing to FileOutputStream", e);
            return false;
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (final IOException e) {
                    Log.error("parseGetFileResponseContent(): Exception while closing fout", e);
                }
            }
        }

        return true;
    }

    public static class NoNetworkException extends RuntimeException {
        public NoNetworkException() {
        }
    }
}
