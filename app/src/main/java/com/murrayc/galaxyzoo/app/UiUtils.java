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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Created by murrayc on 5/21/14.
 */
class UiUtils {

    static String getLocale(final Context context) {
        final Locale locale = context.getResources().getConfiguration().locale;
        final String countryCode = locale.getCountry();
        String result = locale.getLanguage();
        if (!TextUtils.isEmpty(countryCode)) {
            result += "_" + countryCode;
        }

        return result;
    }

    static void fillImageViewFromContentUri(final Context context, final String imageUriStr, final ImageView imageView) {
        if (imageUriStr == null) {
            Log.error("fillImageViewFromContentUri(): imageUriStr is null.");
            return;
        }

        final ContentResolver contentResolver = context.getContentResolver();

        Bitmap bMap = null;
        try {
            final Uri uri = Uri.parse(imageUriStr);
            final InputStream stream = contentResolver.openInputStream(uri);
            bMap = BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            Log.error("BitmapFactory.decodeStream() failed.", e);
            return;
        }

        imageView.setImageBitmap(bMap);
    }

    static void warnAboutNoNetworkConnection(final Activity activity) {
        final Toast toast = Toast.makeText(activity, "No Network Connection", Toast.LENGTH_LONG);
        toast.show();
    }
}
