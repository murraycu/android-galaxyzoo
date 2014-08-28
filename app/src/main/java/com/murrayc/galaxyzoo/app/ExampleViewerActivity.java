package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;



public class ExampleViewerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_viewer);

        final Intent intent = getIntent();
        final String uriStr = intent.getStringExtra(ExampleViewerFragment.ARG_EXAMPLE_URL);

        final Bundle arguments = new Bundle();
        arguments.putString(ExampleViewerFragment.ARG_EXAMPLE_URL, uriStr);

        if (savedInstanceState == null) {
            final ExampleViewerFragment fragment = new ExampleViewerFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }
}
