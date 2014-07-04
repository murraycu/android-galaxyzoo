/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-glom.
 *
 * android-glom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-glom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-glom.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.murrayc.galaxyzoo.app.provider.Item;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 5/16/14.
 */
class ListCursorAdapter extends CursorAdapter {

    private LayoutInflater mLayoutInflater;

    public ListCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* seems reasonable */);

        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.listview_row_fragment_list, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        final String subjectId = cursor.getString(ListFragment.COLUMN_INDEX_SUBJECT_ID);
        final String imageUriStr = cursor.getString(ListFragment.COLUMN_INDEX_LOCATION_THUMBNAIL_URI);

        /**
         * Next set the title of the entry.
         */

        final TextView textView = (TextView) view.findViewById(R.id.item_text);
        if (textView != null) {
            textView.setText(subjectId);
        }

        if (!TextUtils.isEmpty(imageUriStr)) {
            final ImageView imageView = (ImageView) view.findViewById(R.id.item_image);

            ContentResolver contentResolver = context.getContentResolver();

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
}
