package com.murrayc.galaxyzoo.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;


public class ExampleViewerActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_viewer);

        Utils.showToolbar(this);

        final Intent intent = getIntent();
        final String uriStr = intent.getStringExtra(ExampleViewerFragment.ARG_EXAMPLE_URL);

        final Bundle arguments = new Bundle();
        arguments.putString(ExampleViewerFragment.ARG_EXAMPLE_URL, uriStr);

        if (savedInstanceState == null) {
            final ExampleViewerFragment fragment = new ExampleViewerFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

        // Show the Up button in the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
