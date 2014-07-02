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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 5/16/14.
 */
class ListCursorAdapter extends CursorAdapter {

    private final String[] mColumns;
    private List<TextView> mTextViews;

    public ListCursorAdapter(Context context, Cursor c, final String[] columns) {
        super(context, c, 0 /* seems reasonable */);
        mColumns = columns;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final List<Integer> widths; //TODO

        final LinearLayout rowLayout = new LinearLayout(context);
        //rowLayout.setId(View.generateViewId());
        //rowLayout.setTag("content");

        //Create the layout for the row:
        mTextViews = new ArrayList<>();

        final int MAX = 3; //TODO: Be more clever about how we don't use more than the available space.
        for (int i = 0; i < mColumns.length; i++) {
            if (i > MAX)
                break;

            final TextView textView = UiUtils.createTextView(context);

            if (i != MAX) { //Let the last field take all available space.
                textView.setWidth(50); //TODO: widths.get(i));
            }

            //Separate the views with some space:
            if (i != 0) {
                //final float paddingInDp = 16;
                //final float scale = context.getResources().getDisplayMetrics().density;
                //final int dpAsPixels = (int) (paddingInDp * scale + 0.5f); // See http://developer.android.com/guide/practices/screens_support.html#dips-pels
                //textView.setPadding(dpAsPixels /* left */, 0, 0, 0);

                //TODO: Align items so the width is the same for the whole column.
                //final float paddingInDp = R.attr.listPreferredItemPaddingLeft;
                //final float scale = context.getResources().getDisplayMetrics().density;
                //final int dpAsPixels = (int) (paddingInDp * scale + 0.5f); // See http://developer.android.com/guide/practices/screens_support.html#dips-pels
                //textView.setPadding(dpAsPixels /* left */, 0, 0, 0);

                final int size = UiUtils.getStandardItemPadding(context);
                textView.setPadding(size /* left */, 0, 0, 0);
            }

            rowLayout.addView(textView);
            mTextViews.add(textView);
        }

        return rowLayout;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int i = 0;
        for (final TextView textView : mTextViews) {
            //TODO: Keep a list of the LayoutItemFields and do some real rendering here:
            //TODO: Use the correct Cursor.get*() method depending on the column type.
            textView.setText(cursor.getString(i));
            i++;
        }
    }
}
