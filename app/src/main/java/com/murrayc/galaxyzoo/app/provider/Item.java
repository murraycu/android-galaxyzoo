/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by murrayc on 5/28/14.
 */
public class Item {
    public static final String AUTHORITY =
            "com.murrayc.galaxyzoo.app";

    /**
     * The URI for the list of all Items,
     * or part of the URI for a single Item.
     */
    public static final Uri ITEMS_URI = Uri.parse("content://" +
            AUTHORITY + "/" + ItemsContentProvider.URI_PART_ITEM);
    /**
     * The content:// style URI for this item.
     */
    public static final Uri CONTENT_URI = ITEMS_URI;

    /**
     * The URI for the list of all files,
     * or part of the URI for a single file.
     * Clients don't need to build a /file/ URI -
     * they will get a /file/ URI from the Item.Columns.FILE_URI_COLUMN column
     * in the result from a ITEMS_URI query.
     */
    public static final Uri FILE_URI = Uri.parse("content://" +
            AUTHORITY + "/" + ItemsContentProvider.URI_PART_FILE);
    public static final String ITEM_URI_PART = ItemsContentProvider.URI_PART_ITEM;

    public static final class Columns implements BaseColumns {
        //The ID is BaseColumns._ID;
        public static final String DONE = "done";
        public static final String SKIPPED = "skipped";
        public static final String SUBJECT_ID = "subjectId";
        public static final String ZOONIVERSE_ID = "zooniverseId";
        public static final String LOCATION_STANDARD_URI = "locationStandard";
        public static final String LOCATION_THUMBNAIL_URI = "locationThumbnail";
        public static final String LOCATION_INVERTED_URI = "locationInverted";
    }
}
