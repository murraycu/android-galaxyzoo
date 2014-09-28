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

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by murrayc on 5/16/14.
 */
class ListCursorAdapter extends CursorAdapter {

    private final LayoutInflater mLayoutInflater;

    public ListCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* seems reasonable */);

        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.gridview_cell_fragment_list, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        //final String subjectId = cursor.getString(ListFragment.COLUMN_INDEX_SUBJECT_ID);
        final String imageUriStr = cursor.getString(ListFragment.COLUMN_INDEX_LOCATION_THUMBNAIL_URI);
        final boolean done = (cursor.getInt(ListFragment.COLUMN_INDEX_DONE) == 1);
        final boolean uploaded = (cursor.getInt(ListFragment.COLUMN_INDEX_UPLOADED) == 1);
        final boolean favorite = (cursor.getInt(ListFragment.COLUMN_INDEX_FAVOURITE) == 1);


        /**
         * Next set the title of the entry.
         */

        /*
        final TextView textView = (TextView) view.findViewById(R.id.item_text);
        if (textView != null) {
            textView.setText(subjectId);
        }
        */

        if (!TextUtils.isEmpty(imageUriStr)) {
            final ImageView imageView = (ImageView) view.findViewById(R.id.item_image);
            UiUtils.fillImageViewFromContentUri(context, imageUriStr, imageView);

            final LinearLayout iconsPanel = (LinearLayout) view.findViewById(R.id.itemIconsPanel);

            if (!favorite && !done && !uploaded) {
                iconsPanel.setVisibility(View.GONE);
            } else {
                iconsPanel.setVisibility(View.VISIBLE);

                final ImageView checkboxFavorite = (ImageView) view.findViewById(R.id.item_checkboxFavorite);
                checkboxFavorite.setVisibility(favorite ? View.VISIBLE : View.GONE);

                final ImageView checkboxClassified = (ImageView) view.findViewById(R.id.item_checkboxClassified);
                checkboxClassified.setVisibility(done ? View.VISIBLE : View.GONE);

                final ImageView checkboxUploaded = (ImageView) view.findViewById(R.id.item_checkboxUploaded);
                checkboxUploaded.setVisibility(uploaded ? View.VISIBLE : View.GONE);
            }
        }
    }

}
