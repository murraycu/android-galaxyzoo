/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
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

package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.apache.http.NameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;

//import org.apache.http.client.utils.URIBuilder;

/**
 *
 */
public class Utils {


    public static SharedPreferences getPreferences(final Context context) {
        //TODO: Use the application name, or the full domain here?
        //However, changing it now would lose existing preferences, including the login.
        return context.getSharedPreferences("android-galaxyzoo", Context.MODE_PRIVATE);
    }

    public static boolean getNetworkIsConnected(final Context context) {
        boolean connected = false;
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            connected = true;
        }

        return connected;
    }

    public static boolean writeParamsToHttpPost(final HttpURLConnection conn, final List<NameValuePair> nameValuePairs) {
        OutputStream out = null;
        try {
            out = conn.getOutputStream();

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(
                        new OutputStreamWriter(out, "UTF-8"));
                writer.write(getPostDataBytes(nameValuePairs));
                writer.flush();
            } catch (final IOException e) {
                Log.error("writeParamsToHttpPost(): Exception: ", e);
                return false;
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (final IOException e) {
            Log.error("writeParamsToHttpPost(): Exception: ", e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    Log.error("writeParamsToHttpPost(): Exception while closing out", e);
                }
            }
        }

        return true;
    }

    private static String getPostDataBytes(final List<NameValuePair> nameValuePairs) {
        final StringBuilder result = new StringBuilder();
        boolean first = true;

        for (final NameValuePair pair : nameValuePairs) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            try {
                result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.error("getPostDataBytes(): Exception", e);
                return null;
            }
        }

        return result.toString();
    }
}
