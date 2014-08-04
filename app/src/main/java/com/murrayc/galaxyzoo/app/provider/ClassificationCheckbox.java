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
public class ClassificationCheckbox {
    public static final String AUTHORITY =
            Item.AUTHORITY;

    /**
     * The URI for the list of all Items,
     * or part of the URI for a single Item.
     */
    public static final Uri CLASSIFICATION_CHECKBOXES_URI = Uri.parse("content://" +
            AUTHORITY + "/" + ItemsContentProvider.URI_PART_CLASSIFICATION_CHECKBOX);
    /**
     * The content:// style URI for this item.
     */
    public static final Uri CONTENT_URI = CLASSIFICATION_CHECKBOXES_URI;


    public static final class Columns implements BaseColumns {
        //The ID is BaseColumns._ID;
        public static final String CLASSIFICATION_ID = "classificationId";
        public static final String SEQUENCE = "sequence";
        public static final String QUESTION_ID = "questionId";
        public static final String CHECKBOX_ID = "checkboxId";
    }
}
