/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.provider;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.google.common.io.ByteStreams;
import com.murrayc.galaxyzoo.app.BuildConfig;
import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Don't use any of these methods from the main thread.
 *
 * Created by murrayc on 8/25/14.
 */
public final class HttpUtils {

    public static final String HTTP_REQUEST_HEADER_PARAM_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_MURRAYC = "murrayc.com-android-galaxyzoo";
    public static final String HTTP_REQUEST_HEADER_PARAM_ACCEPT = "Accept";
    public static final String HTTP_REQUEST_HEADER_PARAM_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/vnd.api+json; version=1";
    public static final int TIMEOUT_MILLIS = 20000; //20 seconds. Long but not too short for GPRS connections and not endless.

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new UserAgentInterceptor())
            .build();

    public static void throwIfNoNetwork(final Context context) {
        final boolean wifiOnly = LoginUtils.getUseWifiOnly(context);
        if(!getNetworkIsConnected(context, wifiOnly)) {
            //Throw an exception so the caller knows.
            throw new NoNetworkException(wifiOnly);
        }
    }

    public static void throwIfNoNetwork(final Context context, final boolean wifiOnly) {
        if(!getNetworkIsConnected(context, wifiOnly)) {
            //Throw an exception so the caller knows.
            throw new NoNetworkException(wifiOnly);
        }
    }

    /**
     * Don't call this from the main thread because it uses the Account.
     *
     * @param context
     * @return
     */
    private static boolean getNetworkIsConnected(final Context context, final boolean wifiOnly) {
        final Utils.NetworkConnected networkConnected =
                Utils.getNetworkIsConnected(context, wifiOnly);
        return networkConnected.connected;
    }

    /**
     *
     * @param context
     * @param uriFileToCache  A Content URI for a cache file.
     * @param cacheFileUri
     * @return
     * @throws FileCacheException
     */
    public static boolean cacheUriToFileSync(final Context context, final String uriFileToCache, final String cacheFileUri) throws FileCacheException {
        Log.info("cacheUriToFileSync(): uriFileToCache=" + uriFileToCache);

        final Call call = createGetRequestCall(uriFileToCache, false);

        final Response response;
        try {
            response = call.execute();
        } catch (final IOException e) {
            Log.error("cacheUriToFile(): Exception from request.", e);
            throw new FileCacheException("Exception from request.", e);
        }

        if (!response.isSuccessful()) {
            Log.error("cacheUriToFile(): Request was not succcessful.");
            return false;
        }

        try {
            parseGetFileResponseContent(context, response.body().byteStream(), cacheFileUri);
        } catch (final IOException e) {
            Log.error("cacheUriToFile(): parseGetFileResponseContent failed for cache content URI: " + cacheFileUri, e);
            throw new FileCacheException("Exception while parsing response for cache content URI: " + cacheFileUri, e);
        }

        return true;
    }

    public static Call createGetRequestCall(final String uriFileToCache, final boolean cacheResponse) {
        final Request request = new Request.Builder()
                    .url(uriFileToCache)
                    .build();

        // We won't request the same image again if it succeeded once,
        // so don't waste memory or storage caching it.
        // (We are downloading it to our own cache, of course.)
        if (!cacheResponse) {
            request.cacheControl().noCache();
        }

        return client.newCall(request);
    }

    @Nullable
    public static String getPostDataBytes(final List<NameValuePair> nameValuePairs) {
        final StringBuilder result = new StringBuilder();
        boolean first = true;

        for (final NameValuePair pair : nameValuePairs) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            try {
                result.append(URLEncoder.encode(pair.getName(), Utils.STRING_ENCODING));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), Utils.STRING_ENCODING));
            } catch (final UnsupportedEncodingException e) {
                //This is incredibly unlikely for the UTF-8 encoding,
                //so we just log it instead of trying to recover from it.
                Log.error("getPostDataBytes(): Exception", e);
                return null;
            }
        }

        //Log.info("galaxyzoodebug: content:" + result);
        return result.toString();
    }

    @Nullable
    public static String generateAuthorizationHeader(final String authName, final String authApiKey) {
        //See the similar code in Zooniverse's user.coffee source code:
        //https://github.com/zooniverse/Zooniverse/blob/master/src/models/user.coffee#L49
        final String str = authName + ":" + authApiKey;
        byte[] asBytes = null;
        try {
            asBytes = str.getBytes(Utils.STRING_ENCODING);
        } catch (final UnsupportedEncodingException e) {
            //This is incredibly unlikely for the UTF-8 encoding,
            //so we just log it instead of trying to recover from it.
            Log.error("generateAuthorizationHeader(): String.getBytes() failed", e);
            return null;
        }

        return "Basic " + Base64.encodeToString(asBytes, Base64.NO_WRAP);
    }

    @NonNull
    public static String getUserAgent() {
        return HttpUtils.USER_AGENT_MURRAYC + "/" + BuildConfig.VERSION_NAME;
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
            //TODO: Let the caller catch this?
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
            //TODO: Let the caller catch this?
            Log.error("getUriLastModified(): can't openConnection", e);
            return 0;
        }

        final long result = con.getLastModified();
        con.disconnect(); //Otherwise StrictMode says (with an exception) that we leak.
        return result;
    }
    */


    /**
     *
     * @param context
     * @param data
     * @param cacheFileContentUri A Content URI for a cache file.
     * @throws IOException
     */
    private static void parseGetFileResponseContent(final Context context, final byte[] data, final String cacheFileContentUri) throws IOException {
        //Write the content to the file:
        ParcelFileDescriptor pfd = null;
        FileOutputStream fout = null;
        try {
            //Use this instead when using the commented icon-downloading code in IconsCache:
            //fout = new FileOutputStream(cacheFileContentUri);
            //fout.write(data);

            //FileOutputStream doesn't seem to understand content provider URIs:
            pfd = context.getContentResolver().
                    openFileDescriptor(Uri.parse(cacheFileContentUri), "w");
            if (pfd == null) {
                Log.error("parseGetFileResponseContent(): pfd is null.");
            } else {

                fout = new FileOutputStream(pfd.getFileDescriptor());
                fout.write(data);
            }
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
    }

    /**
     *
     * @param context
     * @param data
     * @param cacheFileContentUri A Content URI for a cache file.
     * @throws IOException
     */
    public static void parseGetFileResponseContent(final Context context, final InputStream data, final String cacheFileContentUri) throws IOException {
        //Write the content to the file:
        ParcelFileDescriptor pfd = null;
        FileOutputStream fout = null;
        try {
            //Use this instead when using the commented icon-downloading code in IconsCache:
            //fout = new FileOutputStream(cacheFileContentUri);
            //fout.write(data);

            //FileOutputStream doesn't seem to understand content provider URIs:
            pfd = context.getContentResolver().
                    openFileDescriptor(Uri.parse(cacheFileContentUri), "w");
            if (pfd == null) {
                Log.error("parseGetFileResponseContent(): pfd is null.");
            } else {

                fout = new FileOutputStream(pfd.getFileDescriptor());
                ByteStreams.copy(data, fout);
            }
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
    }

    /**
     * Thrown by methods that required a suitable network connection.
     *
     * This is a RuntimeException because only RuntimeExceptions may be thrown by
     * a ContentProvider.
     */
    public static final class NoNetworkException extends RuntimeException {
        private final boolean wifiOnly;

        /**
         *
         * @param wifiOnly True if there was no wifi connection available when a wifi-only
         *                 connection was required. False if any connection was required but
         *                 none of any kind was available.
         */
        public NoNetworkException(final boolean wifiOnly) {
            this.wifiOnly = wifiOnly;
        }

        public boolean getWifiOnly() {
            return wifiOnly;
        }
    }

    public static class FileCacheException extends Exception {
        FileCacheException(final String detail, final Exception cause) {
            super(detail, cause);
        }
    }

    public static class NameValuePair {
        private final String name;
        private final String value;

        public NameValuePair(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }
    }

    private static class UserAgentInterceptor implements Interceptor {
        @Override
        public Response intercept(final Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header(HTTP_REQUEST_HEADER_PARAM_USER_AGENT, USER_AGENT_MURRAYC)
                    .build();
            return chain.proceed(requestWithUserAgent);        }
    }
}
