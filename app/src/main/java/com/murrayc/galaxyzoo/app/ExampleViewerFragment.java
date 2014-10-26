package com.murrayc.galaxyzoo.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.NetworkImageView;

/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class ExampleViewerFragment extends Fragment {
    public static final String ARG_EXAMPLE_URL = "example-url";
    private View mLoadingView;
    private View mRootView;
    private RequestQueue mRequestQueue = null;
    private ImageLoader mImageLoader = null;

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
    private void loadBitmap(final String strUri, NetworkImageView imageView) {
        showLoadingView(true);

        final Singleton singleton = Singleton.getInstance();
        imageView.setImageUrl(strUri, singleton.getVolleyImageLoader());
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

        final NetworkImageView imageView = (NetworkImageView) mRootView.findViewById(R.id.imageView);
        if (imageView != null) {
            loadBitmap(uriStr, imageView);
        }

        return mRootView;
    }

    private void showLoadingView(boolean show) {
        if (mLoadingView == null) {
            mLoadingView = mRootView.findViewById(R.id.loading_spinner);
        }

        mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


}
