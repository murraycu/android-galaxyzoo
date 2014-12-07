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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class ExampleViewerFragment extends Fragment {
    public static final String ARG_EXAMPLE_URL = "example-url";
    private View mLoadingView = null;
    private View mRootView = null;
    private String mUriStr = null;
    private ImageView mImageView = null;

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

        //Note: We call cancelRequest in onPause() to avoid a leak,
        //as vaguely suggested by the into() documentation.
        Picasso.with(getActivity()).load(strUri).into(imageView, new Callback() {
            @Override
            public void onSuccess() {
                showLoadingView(false);
            }

            @Override
            public void onError() {
                showLoadingView(false);
                Log.error("ExampleViewerFragment.loadBitmap.onError().");
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Bundle bundle = getArguments();
        if (bundle != null) {
            setExampleUrl(bundle.getString(ARG_EXAMPLE_URL));
        }

        mRootView = inflater.inflate(R.layout.fragment_example_viewer, container, false);
        mImageView = (ImageView) mRootView.findViewById(R.id.imageView);

        update();

        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        //Picasso's into() documentation tells us to use cancelRequest() to avoid a leak,
        //though it doesn't suggest where/when to call it:
        //http://square.github.io/picasso/javadoc/com/squareup/picasso/RequestCreator.html#into-android.widget.ImageView-com.squareup.picasso.Callback-
        Picasso.with(getActivity()).cancelRequest(mImageView);
    }

    public void update() {
        if (mImageView != null) {
            loadBitmap(mUriStr, mImageView);
        }
    }

    public void setExampleUrl(final String uriStr) {
        mUriStr = uriStr;
    }

    private void showLoadingView(boolean show) {
        if (mLoadingView == null) {
            mLoadingView = mRootView.findViewById(R.id.loading_spinner);
        }

        mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


}
