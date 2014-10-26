package com.murrayc.galaxyzoo.app.provider;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by murrayc on 8/25/14.
 */
public class HttpUtils {

    private static final String HTTP_REQUEST_HEADER_PARAM_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_MURRAYC = "murrayc.com-android-galaxyzoo";
    private static final int TIMEOUT_MILLIS = 20000; //20 seconds. Long but not too short for GPRS connections and not endless.

    public static void throwIfNoNetwork(final Context context) {
        if (!Utils.getNetworkIsConnected(context)) {
            //Throw an exception so the caller knows.
            throw new NoNetworkException();
        }
    }

    public static HttpURLConnection openConnection(final String strURL) {
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

        //Set a reasonable timeout.
        //Otherwise there is not timeout so we might never know if it fails,
        //so never have the chance to try again.
        conn.setConnectTimeout(TIMEOUT_MILLIS);
        conn.setReadTimeout(TIMEOUT_MILLIS);

        return conn;
    }

    public static class FileCacheRequest extends Request<Boolean> {

        private final String mCacheFileUri;
        private final WeakReference<Context> mContext;
        private final Response.Listener<Boolean> mListener;

        public FileCacheRequest(final Context context, final String uriFileToCache, final String cacheFileUri, final Response.Listener<Boolean> listener) {
            super(Method.GET, uriFileToCache, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.error("FileCacheRequest.onErrorResponse()", error);
                }
            });

            mCacheFileUri = cacheFileUri;
            mContext = new WeakReference<>(context);
            mListener = listener;
        }

        @Override
        protected Response<Boolean> parseNetworkResponse(final NetworkResponse response) {
            boolean result = false;
            if (mContext != null) {
                final Context context = mContext.get();
                if (context != null) {
                    result = parseGetFileResponseContent(context, response.data, mCacheFileUri);
                } else {
                    Log.error("parseNetworkResponse(): context is null.");
                }
            }

            return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(final Boolean response) {
            mListener.onResponse(response);
        }
    }

    /*
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
    */


    private static void setConnectionUserAgent(final HttpURLConnection connection) {
        connection.setRequestProperty(HTTP_REQUEST_HEADER_PARAM_USER_AGENT, USER_AGENT_MURRAYC);
    }

    private static boolean parseGetFileResponseContent(final Context context, final byte[] data, final String cacheFileContentUri) {
        //Write the content to the file:
        ParcelFileDescriptor pfd = null;
        FileOutputStream fout = null;
        try {
            //FileOutputStream doesn't seem to understand content provider URIs:
            pfd = context.getContentResolver().
                    openFileDescriptor(Uri.parse(cacheFileContentUri), "w");

            fout = new FileOutputStream(pfd.getFileDescriptor());
            fout.write(data);
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

            if (pfd != null) {
                try {
                    pfd.close();
                } catch (final IOException e) {
                    Log.error("parseGetFileResponseContent(): Exception while closing pfd", e);
                }
            }
        }

        return true;
    }

    /**
     * This is a RuntimeException because only RuntimeExceptions may be thrown by
     * a ContentProvider.
     */
    public static class NoNetworkException extends RuntimeException {
        public NoNetworkException() {
        }
    }
}
