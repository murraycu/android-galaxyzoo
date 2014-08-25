package com.murrayc.galaxyzoo.app.provider;

import android.os.AsyncTask;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.provider.rest.FileResponseHandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Created by murrayc on 8/25/14.
 */
public class HttpUtils {

    public static class FileCacheAsyncTask extends AsyncTask<String, Integer, Boolean> {

        private Callbacks mCallbacks = null;

        public static interface Callbacks {
            public void onFinished();
        };

        public FileCacheAsyncTask() {

        }

        public FileCacheAsyncTask(final Callbacks callbacks) {
            mCallbacks = callbacks;
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

            if (mCallbacks != null) {
                mCallbacks.onFinished();
            }
        }
    }

    public static boolean cacheUriToFileSync(final String uriFileToCache, final String cacheFileUri) {
        final HttpGet get = new HttpGet(uriFileToCache);
        final ResponseHandler handler = new FileResponseHandler(cacheFileUri);
        return executeHttpRequest(get, handler);
    }

    static boolean executeHttpRequest(final HttpUriRequest request, ResponseHandler<Boolean> handler) {
        Boolean handlerResult = false;
        HttpResponse response = null;
        try {
            final HttpClient client = new DefaultHttpClient();
            //This just leads to an redirect limit exception: client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
            response = client.execute(request);
            handlerResult = handler.handleResponse(response);
        } catch (IOException e) {
            Log.error("exception processing async request", e);
            return false;
        }

        return handlerResult;
    }
}
