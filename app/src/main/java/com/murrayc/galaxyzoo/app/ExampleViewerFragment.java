package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.app.AlertDialog;
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
 * Use the {@link com.murrayc.galaxyzoo.app.ExampleViewerFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ExampleViewerFragment extends Fragment {
    public static final String ARG_EXAMPLE_URL = "example-url";

    //See http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
    private static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String strUri = null;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            strUri = params[0];

            URLConnection connection = null;
            try {
                final URL url = new URL(strUri);
                connection = url.openConnection();
            } catch (IOException e) {
                Log.error("ExampleViewerFragment.BitmapWorkerTask.doInBackground: exception while opening connection", e);
                return null;
            }

            try (final InputStream stream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                Log.error("ExampleViewerFragment.BitmapWorkerTask.doInBackground: exception while using stream", e);
                return null;
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
        }
    }

    /** We need to load the bitmap for the imageview in an async task.
     * This is tedious. It would be far easier if ImageView had a setFromUrl(url) method that did
     * the work asynchronously itself.
     * 
     * @param strUri
     * @param imageView
     */
    private void loadBitmap(final String strUri, ImageView imageView) {
        final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        task.execute(strUri);
    }

    public ExampleViewerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        String uriStr = null;
        final Bundle bundle = getArguments();
        if (bundle != null) {
            uriStr = bundle.getString(ARG_EXAMPLE_URL);
        }

        final Activity activity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());
        View mRootView = inflater.inflate(R.layout.fragment_example_viewer, container, false);

        final ImageView imageView = (ImageView) mRootView.findViewById(R.id.imageView);
        if (imageView != null) {
            loadBitmap(uriStr, imageView);
        }

        return mRootView;
    }


}
