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
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;

/**
 * A singleton that allows our various Activities to share the same data.
 * <p/>
 * This feels hacky, but it a recommended way for Activities to share non-primitive data:
 * http://developer.android.com/guide/faq/framework.html#3
 */
public class Singleton {

    private static Callbacks mCallbacks;

    public static interface Callbacks {
        /**
         * This is called when the Singleton has been initialized.
         * This will be called immediately if the Singleton has
         * already been initialized.
         * When this has been called, getInstance() will return
         * a non-null value.
         */
        public void onInitialized();
    }

    private static Singleton ourInstance = null;
    private DecisionTree mDecisionTree = null;

    private static class InitAsyncTask extends AsyncTask<Context, Void, Void> {
        @Override
        protected Void doInBackground(final Context... params) {
            if (params.length < 1) {
                Log.error("InitAsyncTask: not enough params.");
                return null;
            }

            final Context context = params[0];
            Singleton.ourInstance = new Singleton(context);

            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);

            onLoginTaskFinished();
        }
    }

    private static void onLoginTaskFinished() {
        if (mCallbacks != null) {
            mCallbacks.onInitialized();
        }
    }

    private Singleton(final Context context) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open("sloan_tree.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mDecisionTree = new DecisionTree(inputStream);
    }

    public static void init(final Context context, final Callbacks callbacks) {
        //Just notify the caller if it has already been initialized.
        if (ourInstance != null) {
            callbacks.onInitialized();
            return;
        }

        // Instantiate the Singleton and call our callback later:
        mCallbacks = callbacks;
        final InitAsyncTask task = new InitAsyncTask();
        task.execute(context);
    }

    public static Singleton getInstance() {
        return ourInstance;
    }

    public DecisionTree getDecisionTree() {
        return mDecisionTree;
    }

}
