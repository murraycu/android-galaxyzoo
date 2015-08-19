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

package com.murrayc.galaxyzoo.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

/**
 * An activity showing a single subject. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ClassifyFragment}.
 */
public class QuestionHelpActivity extends BaseActivity implements ItemFragment.Callbacks {

    private String mQuestionId = null;
    private String mGroupId = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_question_help);

        UiUtils.showToolbar(this);

        final Intent intent = getIntent();
        setQuestionId(intent.getStringExtra(BaseQuestionFragment.ARG_QUESTION_ID));
        setGroupId(intent.getStringExtra(QuestionHelpFragment.ARG_GROUP_ID));

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null) {
                //We check to see if the fragment exists already,
                //because apparently it sometimes does exist already when the app has been
                //in the background for some time,
                //at least on Android 5.0 (Lollipop)
                QuestionHelpFragment fragment = (QuestionHelpFragment) fragmentManager.findFragmentById(R.id.container);
                if (fragment == null) {
                    // Create the detail fragment and add it to the activity
                    // using a fragment transaction.
                    final Bundle arguments = new Bundle();
                    arguments.putString(BaseQuestionFragment.ARG_QUESTION_ID,
                            getQuestionId());
                    arguments.putString(QuestionHelpFragment.ARG_GROUP_ID,
                            getGroupId());

                    fragment = new QuestionHelpFragment();
                    fragment.setArguments(arguments);
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, fragment)
                            .commit();
                } else {
                    Log.info("QuestionHelpActivity.onCreate(): The QuestionHelpFragment already existed.");

                    fragment.setQuestionId(getQuestionId());
                    fragment.setGroupId(getGroupId());
                    fragment.update();
                }
            }
        }

        showUpButton();
    }

    public String getQuestionId() {
        return mQuestionId;
    }

    private String getGroupId() {
        return mGroupId;
    }

    //This is not private, so we can use it in tests.
    public void setQuestionId(final String questionId) {
        mQuestionId = questionId;
    }

    private void setGroupId(final String groupId) {
        mGroupId = groupId;
    }

    //We don't actually use this.
    //We just need it because QuestionHelpFragment derives from ItemFragment.
    @Override
    public void navigateToList() {
        final Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    @Override
    public void abandonItem() {
        Log.error("QuestionHelpActivity(): Abandoning item.");
    }
}
