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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.IOException;
import java.io.InputStream;

//import org.apache.http.client.utils.URIBuilder;

/**
 *
 */
public class Utils {

    public static final String STRING_ENCODING = "UTF-8";

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

    public static int getIntPref(final Context context, final int prefKeyResId) {
        final AccountManager mgr = AccountManager.get(context);
        final Account account = getAccount(mgr);
        if (account == null) {
            return 0;
        }

        final String value =
                mgr.getUserData(account, context.getString(prefKeyResId));
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private static Account getAccount(final AccountManager mgr) {
        final Account[] accts = mgr.getAccountsByType(LoginUtils.ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
            return null;
        }

        return accts[0];
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

    public static Uri getItemUri(final String itemId) {
        final Uri.Builder uriBuilder = Item.ITEMS_URI.buildUpon();
        uriBuilder.appendPath(itemId);
        return uriBuilder.build();
    }

    static void copyPrefToAccount(final Context context, final String key, final String value) {
        //Copy the preference to the Account:
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accts = mgr.getAccountsByType(LoginUtils.ACCOUNT_TYPE);
        if((accts == null) || (accts.length < 1)) {
            //Log.error("getAccountLoginDetails(): getAccountsByType() returned no account.");
            return;
        }

        final Account account = accts[0];
        if (account == null) {
            return;
        }

        mgr.setUserData(account, key, value);
    }
}
