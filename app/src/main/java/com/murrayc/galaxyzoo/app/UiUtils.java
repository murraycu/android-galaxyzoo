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


import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Created by murrayc on 5/21/14.
 */
class UiUtils {
    public static int getStandardItemPadding(final Context context) {
        //TODO: Use  listPreferredItemPaddingStart instead, if we can discover what SDK version has it.
        final int[] attrs = new int[]{android.R.attr.listPreferredItemPaddingLeft};
        final TypedArray a = context.obtainStyledAttributes(attrs);
        final int size = a.getDimensionPixelSize(0 /* The first (only) value */,
                -1 /* return this if there is no value */);
        a.recycle();

        //In case the theme didn't have a value:
        if (size == -1) {
            // TODO: This value should be in values*/*.xml files, so it can
            // have an appropriate value for each screen size/dpi.
            final int paddingInDp = 16;
            final float scale = context.getResources().getDisplayMetrics().density;

            //Get the dp as pixels:
            return (int) (paddingInDp * scale + 0.5f); // See http://developer.android.com/guide/practices/screens_support.html#dips-pels
        }

        return size;
    }

    static TextView createTextView(Context context) {
        return new TextView(context);

        /*
        ViewGroup.LayoutParams params = textView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            //params.width = LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        textView.setLayoutParams(params);
        */

        //return textView;
    }

    /**
     * Get the width of the text in pixels, if drawn in the TextView.
     *
     * @param textView
     * @param text
     * @return
     */
    private static float measureText(final TextView textView, final String text) {
        final Paint paint = textView.getPaint();
        final float width = paint.measureText(text); //TODO: Confirm that this is pixels.

        return (float) (width / 1.5); /* TODO: Avoid this hack. */
    }

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
}
