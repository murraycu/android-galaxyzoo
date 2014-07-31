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

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.murrayc.galaxyzoo.app.provider.Item;

/**
 * A fragment representing a single subject.
 * This fragment is either contained in a {@link com.murrayc.galaxyzoo.app.ListActivity}
 * in two-pane mode (on tablets) or a {@link com.murrayc.galaxyzoo.app.DetailActivity}
 * on handsets.
 */
public class QuestionFragment extends ItemFragment  {

    public static final String ARG_QUESTION_ID = "question-id";
    protected String mQuestionId;

    /**
     * A dummy implementation of the {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {

    };

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * A callback interface that all activities containing some fragments must
     * implement. This mechanism allows activities to be notified of
     * navigation selections.
     * <p/>
     * This is the recommended way for activities and fragments to communicate,
     * presumably because, unlike a direct function call, it still keeps the
     * fragment and activity implementations separate.
     * http://developer.android.com/guide/components/fragments.html#CommunicatingWithActivity
     */
    static interface Callbacks {

    }

    private static final int URL_LOADER = 0;
    private long mUserId = -1;
    private String mItemId;
    private Cursor mCursor;

    private View mRootView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public QuestionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            mQuestionId = bundle.getString(ARG_QUESTION_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_question, container, false);
        assert mRootView != null;

        setHasOptionsMenu(true);

        update();

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //final MenuItem menuItem = menu.add(Menu.NONE, R.id.option_menu_item_list, Menu.NONE, R.string.action_list);
        //menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private long getUserId() {
        return mUserId;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    public void update() {
        final Activity activity = getActivity();
        if (activity == null)
            return;


        if (mRootView == null) {
            Log.error("mRootView is null.");
            return;
        }

        final TextView textView = (TextView)mRootView.findViewById(R.id.textView);
        if (textView == null) {
            Log.error("textView is null.");
            return;
        }

        final Singleton singleton = Singleton.getInstance(activity);
        final DecisionTree tree = singleton.getDecisionTree();

        DecisionTree.Question question = null;
        if (TextUtils.isEmpty(mQuestionId)) {
            question = tree.getFirstQuestion();
        } else {
            question = tree.getQuestion(mQuestionId);
        }

        textView.setText(question.getText());
    }


    public String getItemId() {
        return mItemId;
    }
}
