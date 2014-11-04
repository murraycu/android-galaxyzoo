/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;


/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class ExampleViewerFragment extends Fragment {
    public static final String ARG_EXAMPLE_URL = "example-url";
    private View mLoadingView;
    private View mRootView;

    public ExampleViewerFragment() {
        // Required empty public constructor
    }

    /**
     * We need to load the bitmap for the imageview in an async task.
     * This is tedious. It would be far easier if ImageView had a setFromUrl(url) method that did
     * the work asynchronously itself.
     *
     * @param strUri
     * @param imageView
     */
    private void loadBitmap(final String strUri, ImageView imageView) {
        showLoadingView(true);
        final BitmapWorkerTask task = new BitmapWorkerTask(imageView, this);
        task.execute(strUri);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String uriStr = null;
        final Bundle bundle = getArguments();
        if (bundle != null) {
            uriStr = bundle.getString(ARG_EXAMPLE_URL);
        }

        mRootView = inflater.inflate(R.layout.fragment_example_viewer, container, false);

        final ImageView imageView = (ImageView) mRootView.findViewById(R.id.imageView);
        if (imageView != null) {
            loadBitmap(uriStr, imageView);
        }

        return mRootView;
    }

    //See http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
    private static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<ExampleViewerFragment> fragmentReference;

        private String strUri = null;

        public BitmapWorkerTask(final ImageView imageView, final ExampleViewerFragment fragment) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);

            // Use a WeakReference to ensure the ImageView can be garbage collected
            fragment.showLoadingView(true);
            fragmentReference = new WeakReference<>(fragment);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            strUri = params[0];

            URLConnection connection;
            try {
                final URL url = new URL(strUri);
                connection = url.openConnection();
            } catch (IOException e) {
                Log.error("doInBackground(): ExampleViewerFragment.BitmapWorkerTask.doInBackground: exception while opening connection", e);
                return null;
            }

            InputStream stream = null;
            try {
                stream = connection.getInputStream();
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                Log.error("doInBackground(): ExampleViewerFragment.BitmapWorkerTask.doInBackground: exception while using stream", e);
                return null;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        Log.error("doInBackground(): Exception while closing stream.");
                    }
                }
            }
        }

        // Once complete, see if ImageView is still around and set bitmap.
        // This avoids calling the ImageView methods in the non-main thread.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }

            if (fragmentReference != null) {
                final ExampleViewerFragment fragment = fragmentReference.get();
                if (fragment != null) {
                    fragment.showLoadingView(false);
                }
            }
        }
    }

    private void showLoadingView(boolean show) {
        if (mLoadingView == null) {
            mLoadingView = mRootView.findViewById(R.id.loading_spinner);
        }

        mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


}
