package com.murrayc.galaxyzoo.app.provider;

import android.content.Context;
import android.os.AsyncTask;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.rest.FileResponseHandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

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

    public static String getStringFromInputStream(InputStream content) throws IOException {
        final InputStreamReader inputReader = new InputStreamReader(content);
        final BufferedReader reader = new BufferedReader(inputReader);


        int inserted = 0;

        final StringBuilder builder = new StringBuilder();
        for (String line = null; (line = reader.readLine()) != null;) {
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    static InputStream httpGetRequest(final Context context, final String strUri) {
        throwIfNoNetwork(context);

        final HttpURLConnection conn = openConnection(strUri);
        if (conn == null) {
            return null;
        }

        try
        {
            // This is the default: conn.setRequestMethod("GET");

            //Calling getInputStream() causes the request to actually be sent.
            final InputStream in = conn.getInputStream();
            if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.error("httpGetRequest(): response code: " + conn.getResponseCode());
                return null;
            }

            return in;
        } catch (final IOException e) {
            Log.error("httpGetRequest(): exception during HTTP connection", e);

            return null;
        }
    }

    static void throwIfNoNetwork(final Context context) {
        if(!Utils.getNetworkIsConnected(context)) {
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
            conn = (HttpURLConnection)url.openConnection();
            setConnectionUserAgent(conn);
        } catch (final IOException e) {
            Log.error("openConnection(): exception during HTTP connection", e);

            return null;
        }

        return conn;
    }

    public static class FileCacheAsyncTask extends AsyncTask<String, Integer, Boolean> {

        public FileCacheAsyncTask() {

        }

        @Override
        protected Boolean doInBackground(final String... params) {
            if (params.length < 2) {
                Log.error("LoginTask: not enough params.");
                return false;
            }

            //TODO: Just set these in the constructor?
            final String uriFileToCache = params[0];
            final String cacheFileUri = params[1];

            return HttpUtils.cacheUriToFileSync(uriFileToCache, cacheFileUri);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
        }
    }

    public static boolean cacheUriToFileSync(final String uriFileToCache, final String cacheFileUri) {
        final HttpGet get = new HttpGet(uriFileToCache);
        final ResponseHandler<Boolean> handler = new FileResponseHandler(cacheFileUri);
        return executeHttpRequest(get, handler);
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


    private static long getUriLastModified(final String strUrl)
    {
        final URL url;
        try {
            url = new URL(strUrl);
        } catch (final MalformedURLException e) {
            Log.error("getUriLastModified(): can't instantiate URL", e);
            return 0;
        }

        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = null;
        try {
            //Note: This seems to succeed even if there is no internet connection,
            //for instance if the device is in airplane mode.
            //getLastModified() then returns 0;
            con = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            Log.error("getUriLastModified(): can't openConnection", e);
            return 0;
        }

        return con.getLastModified();
    }

    static boolean executeHttpRequest(final HttpUriRequest request, ResponseHandler<Boolean> handler) {
        setRequestUserAgent(request);

        Boolean handlerResult = false;
        HttpResponse response = null;
        try {
            final HttpClient client = new DefaultHttpClient();
            //This just leads to an redirect limit exception: client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
            response = client.execute(request);
            handlerResult = handler.handleResponse(response);
        } catch (IOException e) {
            Log.error("executeHttpRequest(): exception processing async request", e);
            return false;
        }

        return handlerResult;
    }

    public static void setRequestUserAgent(final HttpUriRequest get) {
        get.setHeader(HTTP_REQUEST_HEADER_PARAM_USER_AGENT, USER_AGENT_MURRAYC);
    }

    public static void setConnectionUserAgent(final HttpURLConnection connection) {
        connection.setRequestProperty(HTTP_REQUEST_HEADER_PARAM_USER_AGENT, USER_AGENT_MURRAYC);
    }
}
