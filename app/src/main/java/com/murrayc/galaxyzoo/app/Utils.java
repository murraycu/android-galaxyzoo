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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

//import org.apache.http.client.utils.URIBuilder;

/**
 *
 */
public class Utils {


    public static final String STRING_ENCODING = "UTF-8";

    public static SharedPreferences getPreferences(final Context context) {
        //TODO: Use the application name, or the full domain here?
        //However, changing it now would lose existing preferences, including the login.
        return context.getSharedPreferences("android-galaxyzoo", Context.MODE_PRIVATE);
    }

    public static boolean getNetworkIsConnected(final Context context) {
        boolean connected = false;
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) {
            //This happens during our test case, probably because the MockContext doesn't support
            //this, so let's ignore it.
            return false;
        }

        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            connected = true;
        }

        return connected;
    }

    public static void setBytesPref(final Context context, int prefKeyId, byte[] bytes) {
        String asString = null;
        if (bytes != null && (bytes.length != 0)) {
            final byte[] asBytesBase64 = Base64.encode(bytes, Base64.DEFAULT);
            try {
                asString = new String(asBytesBase64, STRING_ENCODING);
            } catch (final UnsupportedEncodingException e) {
                Log.error("getEncryptionKey(): new String() failed.", e);
            }
        }

        final SharedPreferences prefs = getPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(prefKeyId), asString);
        editor.apply();

    }

    static byte[] getBytesPref(final Context context, final int prefsKeyId) {
        final SharedPreferences prefs = getPreferences(context);
        final String asString = prefs.getString(context.getString(prefsKeyId), null);
        if (!TextUtils.isEmpty(asString)) {
            final byte[] asBytes;
            try {
                asBytes = asString.getBytes(STRING_ENCODING);
            } catch (UnsupportedEncodingException e) {
                Log.error("getEncryptionKey(): String.getBytes() failed.", e);
                return null;
            }

            return Base64.decode(asBytes, Base64.DEFAULT);
        }

        return null;
    }

    public static int getIntPref(final Context context, final int prefKeyResId) {
        final String prefKey = context.getString(prefKeyResId);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        //Android's PreferencesScreen XMl has no way to specify an integer rather than a string,
        //so we parse it here.
        final String str = prefs.getString(prefKey, null);

        //Avoid a NumberFormatException
        if (TextUtils.isEmpty(str)) {
            return 0;
        }

        return Integer.parseInt(str);
    }

    static InputStream openAsset(final Context context, final String filename) {
        try {
            return context.getAssets().open(filename);
        } catch (final IOException e) {
            //Don't log this because we expect the file to not exist sometimes,
            //and the caller will just check for a null result to know that.
            return null;
        }
    }

    public static void abandonItem(final Context context, final String itemId) {
        final Uri itemUri = ContentUris.withAppendedId(
                Item.ITEMS_URI, Integer.parseInt(itemId));

        final ContentResolver resolver = context.getContentResolver();
        final int affected = resolver.delete(itemUri, null, null);
        if (affected != 1) {
            Log.error("abandonItem(): Unexpected number of rows affected: " + affected);
        }
    }
}
