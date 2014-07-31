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

import java.io.IOException;
import java.io.InputStream;

/**
 * A singleton that allows our various Activities to share the same data.
 * <p/>
 * This feels hacky, but it a recommended way for Activities to share non-primitive data:
 * http://developer.android.com/guide/faq/framework.html#3
 */
public class Singleton {

    private static Singleton ourInstance = null;

    private DecisionTree mDecisionTree = null;

    private Singleton(final Context context) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open("sloan_tree.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mDecisionTree = new DecisionTree(inputStream);
    }

    public static Singleton getInstance(final Context context) {
        if (ourInstance == null) {
            ourInstance = new Singleton(context);
        }

        return ourInstance;

    }

    public DecisionTree getDecisionTree() {
        return mDecisionTree;
    }

}
