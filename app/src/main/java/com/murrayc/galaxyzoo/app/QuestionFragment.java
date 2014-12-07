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
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.murrayc.galaxyzoo.app.provider.ClassificationAnswer;
import com.murrayc.galaxyzoo.app.provider.ClassificationCheckbox;
import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A fragment representing a single subject.
 * This fragment is either contained in a {@link com.murrayc.galaxyzoo.app.ListActivity}
 * in two-pane mode (on tablets) or a {@link com.murrayc.galaxyzoo.app.ClassifyActivity}
 * on handsets.
 */
public class QuestionFragment extends BaseQuestionFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_QUESTION_CLASSIFICATION_IN_PROGRESS = "classification-in-progress";

    private static final int URL_LOADER = 0;

    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    /* private static final int COLUMN_INDEX_ID = 0; */
    private static final int COLUMN_INDEX_ZOONIVERSE_ID = 1;

    /**
     * A dummy implementation of the {@link com.murrayc.galaxyzoo.app.ListFragment.Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        public void onClassificationFinished() {
        }
    };

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    private final String[] mColumns = {Item.Columns._ID, Item.Columns.ZOONIVERSE_ID};

    // A map of checkbox IDs to buttons.
    private final Map<String, ToggleButton> mCheckboxButtons = new HashMap<>();
    private Cursor mCursor = null;
    private String mZooniverseId = null; //Only used for the talk URI so far.
    private boolean mLoaderFinished = false;

    private ClassificationInProgress mClassificationInProgress = new ClassificationInProgress();
    private QuestionLinearLayout mRootView = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public QuestionFragment() {
    }

    private String getZooniverseId() {
        return mZooniverseId;
    }

    private void setZooniverseId(final String zooniverseId) {
        mZooniverseId = zooniverseId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //The item ID in savedInstanceState (from onSaveInstanceState())
        //overrules the item ID in the intent's arguments,
        //because the fragment may have been created with the virtual "next" ID,
        //but we replace that with the actual ID,
        //and we don't want to lost that actual ID when the fragment is recreated after
        //rotation.
        if (savedInstanceState != null) {
            setQuestionId(savedInstanceState.getString(ARG_QUESTION_ID));

            //Get the classification in progress too,
            //instead of losing it when we rotate:
            mClassificationInProgress = savedInstanceState.getParcelable(ARG_QUESTION_CLASSIFICATION_IN_PROGRESS);
        } else {
            final Bundle bundle = getArguments();
            if (bundle != null) {
                setQuestionId(bundle.getString(ARG_QUESTION_ID));
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    protected void setItemId(String itemId) {
        super.setItemId(itemId);

        /*
         * Initializes the CursorLoader. The URL_LOADER value is eventually passed
         * to onCreateLoader().
         * This lets us get the Zooniverse ID for the item, for use in the discussion page's URI.
         * We use restartLoader(), instead of initLoader(),
         * so we can refresh this fragment to show a different subject,
         * even when using the same query ("next") to do that.
         */
        mLoaderFinished = false; //Don't update() until this is ready.
        getLoaderManager().restartLoader(URL_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = (QuestionLinearLayout) inflater.inflate(R.layout.fragment_question, container, false);
        assert mRootView != null;

        setHasOptionsMenu(true);

        initializeSingleton();

        //This will be called later by updateIfReady(): update();

        return mRootView;
    }

    @Override
    void onSingletonInitialized() {
        super.onSingletonInitialized();

        updateIfReady();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        //Save state to be used later by onCreate().
        //If we don't do this then we we will lose the current question ID that we are using.
        //This way we can get the current question ID back again in onCreate().
        //Otherwise, on rotation, onCreateView() will just get the first question ID, if any, that was first used
        //to create the fragment.
        outState.putString(ARG_QUESTION_ID, getQuestionId());
        outState.putParcelable(ARG_QUESTION_CLASSIFICATION_IN_PROGRESS, mClassificationInProgress);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.actionbar_menu_question, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //Before the menu item is displayed,
        //make sure that the checked state is correct:
        final MenuItem item = menu.findItem(R.id.option_menu_item_favorite);
        if (item != null) {
            boolean checked = false;
            if (mClassificationInProgress != null) {
                checked = mClassificationInProgress.isFavorite();
            }
            item.setChecked(checked);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_examples:
                doExamples();
                return true;
            case R.id.option_menu_item_favorite:
                final boolean checked = !item.isChecked();

                //Note:
                //"Icon menu" (TODO: What is that?) items don't actually show a checked state,
                //but it seems to work in the menu though not as an action in the action bar.
                //See http://developer.android.com/guide/topics/ui/menus.html#checkable
                item.setChecked(checked);

                //TODO: Use pretty icons instead:
                /*
                //Show an icon to indicate checkedness instead:
                //See http://developer.android.com/guide/topics/ui/menus.html#checkable
                if (checked) {
                    item.setIcon(android.R.drawable.ic_menu_save); //A silly example.
                } else {
                    item.setIcon(android.R.drawable.ic_menu_add); //A silly example.
                }
                */

                mClassificationInProgress.setFavorite(checked);
                return true;
            case R.id.option_menu_item_restart: {
                restartClassification();
                return true;
            }
            case R.id.option_menu_item_discuss: {
                UiUtils.openDiscussionPage(getActivity(), getZooniverseId());
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doExamples() {
        final Intent intent = new Intent(getActivity(), QuestionHelpActivity.class);
        intent.putExtra(ARG_QUESTION_ID, getQuestionId());
        startActivity(intent);
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

        if (getSingleton() == null) {
            //The parent fragment's onSingletonInitialized has been called
            //but this fragment's onSingletonInitialized hasn't been called yet.
            //That's OK. update() will be called, indirectly, later by this fragment's onSingletonInitialized().
            return;
        }

        if (mRootView == null) {
            //This can happen when update() is called by the parent fragment
            //after this fragment has been instantiated after an orientation change,
            //but before onCreateView() has been called. It's not a problem
            //because onCreateView() will call this method again after setting mRootView.
            //Log.error("QuestionFragment.update(): mRootView is null.");
            return;
        }

        final DecisionTree.Question question = getQuestion();

        //Show the title:
        final TextView textViewTitle = (TextView) mRootView.findViewById(R.id.textViewTitle);
        if (textViewTitle == null) {
            Log.error("textViewTitle is null.");
            return;
        }
        textViewTitle.setText(question.getTitle());

        //Show the text:
        final TextView textViewText = (TextView) mRootView.findViewById(R.id.textViewText);
        if (textViewText == null) {
            Log.error("textViewText is null.");
            return;
        }
        textViewText.setText(question.getText());


        final TableLayout layoutAnswers = (TableLayout) mRootView.findViewById(R.id.layoutAnswers);
        if (layoutAnswers == null) {
            Log.error("layoutAnswers is null.");
            return;
        }

        layoutAnswers.removeAllViews();
        layoutAnswers.setShrinkAllColumns(true);
        layoutAnswers.setStretchAllColumns(true);

        //Checkboxes:
        mCheckboxButtons.clear();
        final int COL_COUNT = 4;
        int col = 1;
        int rows = 0;
        TableRow row = null;
        final LayoutInflater inflater = LayoutInflater.from(activity);
        for (final DecisionTree.Checkbox checkbox : question.getCheckboxes()) {
            //Start a new row if necessary:
            if (row == null) {
                row = addRowToTable(activity, layoutAnswers);
                rows++;
            }

            final ToggleButton button = (ToggleButton)inflater.inflate(R.layout.question_answer_checkbox, null);

            //Use just the highlighting (line, color, etc) to show that it's selected,
            //instead of On/Off, so we don't need a separate label.
            //TODO: Use the icon. See http://stackoverflow.com/questions/18598255/android-create-a-toggle-button-with-image-and-no-text
            //TODO: Avoid the highlight bar thing at the bottom being drawn over the text.
            button.setText(checkbox.getText());
            button.setTextOn(checkbox.getText());
            button.setTextOff(checkbox.getText());

            insertButtonInRow(activity, row, button);

            final BitmapDrawable icon = getIcon(activity, checkbox);
            button.setCompoundDrawables(null, icon, null, null);

            mCheckboxButtons.put(checkbox.getId(), button);

            if (col < COL_COUNT) {
                col++;
            } else {
                col = 1;
                row = null;
            }
        }

        //Answers:
        for (final DecisionTree.Answer answer : question.getAnswers()) {
            //Start a new row if necessary:
            if (row == null) {
                row = addRowToTable(activity, layoutAnswers);
                rows++;
            }

            final Button button = createAnswerButton(activity, answer);
            insertButtonInRow(activity, row, button);

            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    onAnswerButtonClicked(question.getId(), answer.getId());
                }
            });

            if (col < COL_COUNT) {
                col++;
            } else {
                col = 1;
                row = null;
            }
        }

        //Add empty remaining cells, to avoid the other cells from expanding to fill the space,
        //because we want them to line up with the same cells above and below.
        if ((row != null) && (rows > 1)) {
            final int remaining_in_row = COL_COUNT - col + 1;
            for (int i = 0; i < remaining_in_row; i++) {
                //TODO: We could use Space instead of FrameLayout when using API>14.
                final FrameLayout placeholder = new FrameLayout(activity);
                insertButtonInRow(activity, row, placeholder);
            }
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            /* This wastes even more space to be even more consistent:
            //Make sure there are always at least 2 rows,
            //so we request roughly the same amount of space each time:
            if (rows < 2) {
                row = addRowToTable(activity, layoutAnswers);

                final DecisionTree.Answer answer = new DecisionTree.Answer("bogus ID", "bogus title", getArbitraryIconId(), null, 0);
                final Button button = createAnswerButton(activity, answer);
                button.setVisibility(View.INVISIBLE); //It won't be seen, but it's size will be used.
                insertButtonInRow(activity, row, button);
            }
            */


            //This will be used in a later onLayout(),
            //so we will know the correct height at least during the second classification,
            mRootView.setRowsCountForMaxHeightExperienced(rows);

            //Try to keep the height consistent, to avoid the user seeing everything moving about.
            final int min = mRootView.getMaximumHeightExperienced(rows);
            if (min > 0) {
                mRootView.setMinimumHeight(min);
            }
        } else {
            //Ignore any previously-set minimum height,
            //to stop the portrait-mode's layout from affecting the layout-mode's layout:
            mRootView.setMinimumHeight(0);
        }
    }

    private static TableRow addRowToTable(final Activity activity, final TableLayout layoutAnswers) {
        final TableRow row = new TableRow(activity);

        final TableLayout.LayoutParams params =
                new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.MATCH_PARENT);

        //Add a top margin between this row and any row above it:
        if (layoutAnswers.getChildCount() > 0) {
            final int margin = UiUtils.getPxForDpResource(activity, R.dimen.tiny_gap);
            params.setMargins(0, margin, 0, 0);
        }

        layoutAnswers.addView(row, params);
        return row;
    }

    private static void insertButtonInRow(final Context context, final TableRow row, final View button) {
        final TableRow.LayoutParams params =
                new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f /* weight */);
        //Use as little padding as possible at the left and right because the button
        //will usually get extra space from the TableLayout anyway,
        //but we want to avoid ugly line-breaks when the text is long (such as in translations).

        //Space around the inside of the buttons:
        //When we use smaller dp values, there seems to be no padding at the sides at all,
        //probably because the edges of the button are actually dependent on the standard background
        //image for buttons.
        //2 * standard_margin is nicer, but there is usually more, because the buttons expand
        //and a too-small margin is better than splitting a word across lines.
        final int padding = UiUtils.getPxForDpResource(context, R.dimen.small_margin);
        button.setPadding(padding, button.getPaddingTop(), padding, padding);

        if(row.getChildCount() > 0) {
            //Space between the buttons:
            final int margin = UiUtils.getPxForDpResource(context, R.dimen.tiny_gap);
            params.setMargins(margin, 0, 0, 0);
            // When using the standard background drawable (not our custom background color
            // which replaces it) This reduces the space caused by the standard background drawable,
            // but negative margins are unmaintainable voodoo:
            // params.setMargins(-4, 0, -4, 0);
        }

        row.addView(button, params);
    }

    private Button createAnswerButton(Activity activity, DecisionTree.Answer answer) {
        final LayoutInflater inflater = LayoutInflater.from(activity);
        final Button button = (Button)inflater.inflate(R.layout.question_answer_button, null);
        button.setText(answer.getText());

        final BitmapDrawable icon = getIcon(activity, answer);
        button.setCompoundDrawables(null, icon, null, null);
        //There is still some padding: button.setCompoundDrawablePadding(0); //UiUtils.getPxForDpResource(activity, R.dimen.standard_margin));
        return button;
    }

    private void onAnswerButtonClicked(final String questionId, final String answerId) {
        if (questionId == null) {
            Log.error("onAnswerButtonClicked: questionId was null.");
            return;
        }

        //TODO: Move this logic to the parent ClassifyFragment?

        //Save the answer so we can upload it when the classification is finished.
        storeAnswer(questionId, answerId);

        //Open the discussion page if the user chose that.
        if ((TextUtils.equals(questionId, com.murrayc.galaxyzoo.app.Config.QUESTION_ID_DISCUSS)) &&
                (TextUtils.equals(answerId, com.murrayc.galaxyzoo.app.Config.ANSWER_ID_DISCUSS_YES))) {
            //Open a link to the discussion page.
            UiUtils.openDiscussionPage(getActivity(), getZooniverseId());
        }

        //Show the next question.
        showNextQuestion(questionId, answerId);
    }

    /**
     * Show the next question,
     * saving the whole classification and showing a new subject if necessary.
     *
     * @param questionId
     * @param answerId
     */
    private void showNextQuestion(final String questionId, final String answerId) {
        final Singleton singleton = getSingleton();
        final DecisionTree tree = singleton.getDecisionTree();

        final DecisionTree.Question nextQuestion = tree.getNextQuestionForAnswer(questionId, answerId);
        if (nextQuestion == null) {
            //The classification is finished.
            //We save it to the ContentProvider, which will upload it.
            //
            //Set the questionID to null to prevent us from starting to save this again
            //if the user presses the "Done" button again while we are waiting for our AsyncTask.
            //(see our check for a null questionId at the start of this method.)
            final SaveClassificationTask task = new SaveClassificationTask(this, mClassificationInProgress);
            task.execute();
            return;
        }

        final String nextQuestionId = nextQuestion.getId();

        //Skip the "Discuss" question, depending on the setting:
        if (!TextUtils.isEmpty(nextQuestionId)
                && nextQuestionId.equals(Config.QUESTION_ID_DISCUSS)
                && !Utils.getShowDiscussQuestionFromSharedPrefs(getActivity())) {
            //Add a "No" for the Discuss question without even showing the question:
            storeAnswer(nextQuestionId, Config.ANSWER_ID_DISCUSS_NO);

            showNextQuestion(nextQuestionId, Config.ANSWER_ID_DISCUSS_NO);
            return;
        }

        setQuestionId(nextQuestionId);
        update();
    }

    private void storeAnswer(final String questionId, final String answerId) {
        List<String> checkboxes = null;

        //Get the selected checkboxes too:
        final Singleton singleton = getSingleton();
        final DecisionTree tree = singleton.getDecisionTree();
        final DecisionTree.Question question = tree.getQuestion(questionId);
        if ((question != null) && question.hasCheckboxes()) {
            checkboxes = new ArrayList<>();
            for (final DecisionTree.Checkbox checkbox : question.getCheckboxes()) {
                final String checkboxId = checkbox.getId();
                final ToggleButton button = mCheckboxButtons.get(checkboxId);
                if ((button != null) && button.isChecked()) {
                    checkboxes.add(checkboxId);
                }
            }
        }

        //Remember the answer:
        mClassificationInProgress.add(questionId, answerId, checkboxes);
    }

    private void wipeClassification() {
        mClassificationInProgress = new ClassificationInProgress();
        setQuestionId(null);
    }

    private void restartClassification() {
        wipeClassification();
        update();
    }

    private static class SaveClassificationTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<QuestionFragment> fragmentReference;
        private final ClassificationInProgress classificationInProgress;

        SaveClassificationTask(final QuestionFragment fragment, final ClassificationInProgress classificationInProgress) {
            this.fragmentReference = new WeakReference<>(fragment);
            this.classificationInProgress = classificationInProgress;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (fragmentReference == null) {
                return null;
            }

            final QuestionFragment fragment = fragmentReference.get();
            if (fragment == null) {
                return null;
            }

            fragment.saveClassificationSync(classificationInProgress);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (fragmentReference == null) {
                return;
            }

            final QuestionFragment fragment = fragmentReference.get();
            if (fragment == null) {
                return;
            }

            //Finish the classification:
            fragment.wipeClassification();

            //TODO: Do something else for tablet UIs that share the activity.
            fragment.mCallbacks.onClassificationFinished();

        }

    }

    /**
     * Avoid calling this from the main (UI) thread - StrictMode doesn't like it on at least API 15
     * and API 16.
     *
     * @param classificationInProgress
     */
    private void saveClassificationSync(final ClassificationInProgress classificationInProgress) {
        final String itemId = getItemId();
        if (TextUtils.equals(itemId, ItemsContentProvider.URI_PART_ITEM_ID_NEXT)) {
            Log.error("QuestionFragment.saveClassification(): Attempting to save with the 'next' ID.");
            return;
        }

        final Activity activity = getActivity();
        if (activity == null)
            return;

        final ContentResolver resolver = activity.getContentResolver();

        // Add the related Classification Answers:
        // Use a ContentProvider operation to perform operations together,
        // either completely or not at all, as a transaction.
        // This should prevent an incomplete classification from being uploaded
        // before we have finished adding it.
        final ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        int sequence = 0;
        final List<ClassificationInProgress.QuestionAnswer> answers = classificationInProgress.getAnswers();
        if (answers != null) {
            for (final ClassificationInProgress.QuestionAnswer answer : answers) {
                ContentProviderOperation.Builder builder =
                        ContentProviderOperation.newInsert(ClassificationAnswer.CLASSIFICATION_ANSWERS_URI);
                final ContentValues valuesAnswers = new ContentValues();
                valuesAnswers.put(ClassificationAnswer.Columns.ITEM_ID, itemId);
                valuesAnswers.put(ClassificationAnswer.Columns.SEQUENCE, sequence);
                valuesAnswers.put(ClassificationAnswer.Columns.QUESTION_ID, answer.getQuestionId());
                valuesAnswers.put(ClassificationAnswer.Columns.ANSWER_ID, answer.getAnswerId());
                builder.withValues(valuesAnswers);
                ops.add(builder.build());

                //For instance, if the question has multiple-choice checkboxes to select before clicking
                //the "Done" answer:
                final List<String> checkboxIds = answer.getCheckboxIds();
                if (checkboxIds != null) {
                    for (final String checkboxId : checkboxIds) {
                        builder =
                                ContentProviderOperation.newInsert(ClassificationCheckbox.CLASSIFICATION_CHECKBOXES_URI);
                        final ContentValues valuesCheckbox = new ContentValues();
                        valuesCheckbox.put(ClassificationCheckbox.Columns.ITEM_ID, itemId);
                        valuesCheckbox.put(ClassificationCheckbox.Columns.SEQUENCE, sequence);
                        valuesCheckbox.put(ClassificationCheckbox.Columns.QUESTION_ID, answer.getQuestionId());
                        valuesCheckbox.put(ClassificationCheckbox.Columns.CHECKBOX_ID, checkboxId);
                        builder.withValues(valuesCheckbox);
                        ops.add(builder.build());
                    }
                }

                sequence++;
            }
        }

        //Mark the Item (Subject) as done:
        final Uri.Builder uriBuilder = Item.ITEMS_URI.buildUpon();
        uriBuilder.appendPath(getItemId());
        final ContentProviderOperation.Builder builder =
                ContentProviderOperation.newUpdate(uriBuilder.build());
        final ContentValues values = new ContentValues();
        values.put(Item.Columns.DONE, true);
        values.put(Item.Columns.DATETIME_DONE, getCurrentDateTimeAsIso8601());
        values.put(Item.Columns.FAVORITE, classificationInProgress.isFavorite());
        builder.withValues(values);
        ops.add(builder.build());

        try {
            resolver.applyBatch(ClassificationAnswer.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            Log.error("QuestionFragment. saveClassification(): Exception from applyBatch()", e);
        }

        //The ItemsContentProvider will upload the classification later.
    }

    private String getCurrentDateTimeAsIso8601() {
        final Date now = new Date();
        //TODO: Is there a simpler way of getting an ISO-8601-formatted date,
        //or at least a way to avoid writing the format out manually here?
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(now);
    }

    private void updateFromCursor() {
        if (mCursor == null) {
            Log.error("mCursor is null.");
            return;
        }

        final Activity activity = getActivity();
        if (activity == null)
            return;

        if (mCursor.getCount() <= 0) { //In case the query returned no rows.
            Log.error("QuestionFragment.updateFromCursor(): The ContentProvider query returned no rows.");
            UiUtils.warnAboutMissingNetwork(activity);

            return;
        }

        if (mCursor.getColumnCount() <= 0) { //In case the query returned no columns.
            Log.error("The ContentProvider query returned no columns.");
            return;
        }

        mCursor.moveToFirst(); //There should only be one anyway.

        //Look at each group in the layout:
        if (mRootView == null) {
            Log.error("QuestionFragment.updateFromCursor(): mRootView is null.");
            return;
        }

        final String zooniverseId = mCursor.getString(COLUMN_INDEX_ZOONIVERSE_ID);
        setZooniverseId(zooniverseId);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        if (loaderId != URL_LOADER) {
            return null;
        }

        final String itemId = getItemId();
        if (TextUtils.isEmpty(itemId)) {
            return null;
        }

        final Activity activity = getActivity();

        final Uri.Builder builder = Item.CONTENT_URI.buildUpon();
        builder.appendPath(itemId);

        return new CursorLoader(
                activity,
                builder.build(),
                mColumns,
                null, // No where clause, return all records. We already specify just one via the itemId in the URI
                null, // No where clause, therefore no where column values.
                null // Use the default sort order.
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        updateFromCursor();

        mLoaderFinished = true;
        updateIfReady();

        // Avoid this being called twice (actually multiple times), which seems to be an Android bug:
        // See http://stackoverflow.com/questions/14719814/onloadfinished-called-twice
        // and https://code.google.com/p/android/issues/detail?id=63179
        getLoaderManager().destroyLoader(URL_LOADER);
    }

    private void updateIfReady() {
        if (mLoaderFinished && (getSingleton() != null)) {
            update();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        /*
         * Clears out our reference to the Cursor.
         * This prevents memory leaks.
         */
        mCursor = null;
    }

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

        /**
         * We call this when the classification has been finished and saved.
         */
        public void onClassificationFinished();
    }

    //This is just public so we can test it.
    /**
     * This lets us store the classification's answers
     * during the classification. Alternatively,
     * we could insert the answers into the ContentProvider along the
     * way, but this lets us avoid having half-complete classifications
     * in the content provider.
     */
    static public class ClassificationInProgress implements Parcelable {
        public static final Parcelable.Creator<ClassificationInProgress> CREATOR
                = new Parcelable.Creator<ClassificationInProgress>() {
            public ClassificationInProgress createFromParcel(Parcel in) {
                return new ClassificationInProgress(in);
            }

            public ClassificationInProgress[] newArray(int size) {
                return new ClassificationInProgress[size];
            }
        };
        private final List<QuestionAnswer> answers;
        private boolean favorite = false;

        public ClassificationInProgress() {
            answers = new ArrayList<>();
        }

        public ClassificationInProgress(final Parcel in) {
            this.answers = in.createTypedArrayList(QuestionAnswer.CREATOR);

            favorite = (in.readInt() == 1);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            final ClassificationInProgress that = (ClassificationInProgress) o;

            if (favorite != that.favorite)
                return false;

            if (answers != null ? !answers.equals(that.answers) : that.answers != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = answers != null ? answers.hashCode() : 0;
            result = 31 * result + (favorite ? 1 : 0);
            return result;
        }

        public void add(final String questionId, final String answerId, final List<String> checkboxIds) {
            answers.add(new QuestionAnswer(questionId, answerId, checkboxIds));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, int flags) {
            dest.writeTypedList(answers);
            dest.writeInt(favorite ? 1 : 0);
        }

        List<QuestionAnswer> getAnswers() {
            return answers;
        }

        boolean isFavorite() {
            return favorite;
        }

        public void setFavorite(boolean favorite) {
            this.favorite = favorite;
        }

        static private class QuestionAnswer implements Parcelable {
            public static final Parcelable.Creator<QuestionAnswer> CREATOR
                    = new Parcelable.Creator<QuestionAnswer>() {
                public QuestionAnswer createFromParcel(Parcel in) {
                    return new QuestionAnswer(in);
                }

                public QuestionAnswer[] newArray(int size) {
                    return new QuestionAnswer[size];
                }
            };

            // The question that was answered.
            private final String questionId;

            // The Answer that was chosen.
            private final String answerId;

            // Any checkboxes that were selected before the answer (usually "Done") was chosen.
            private List<String> checkboxIds;

            public QuestionAnswer(final String questionId, final String answerId, final List<String> checkboxIds) {
                this.questionId = questionId;
                this.answerId = answerId;
                this.checkboxIds = checkboxIds;
            }

            private QuestionAnswer(final Parcel in) {
                //Keep this in sync with writeToParcel().
                this.questionId = in.readString();
                this.answerId = in.readString();

                this.checkboxIds = in.createStringArrayList();
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o)
                    return true;

                if (o == null || getClass() != o.getClass())
                    return false;

                final QuestionAnswer that = (QuestionAnswer) o;

                if (answerId != null ? !answerId.equals(that.answerId) : that.answerId != null)
                    return false;

                if (checkboxIds != null ? !checkboxIds.equals(that.checkboxIds) : that.checkboxIds != null)
                    return false;

                if (questionId != null ? !questionId.equals(that.questionId) : that.questionId != null)
                    return false;

                return true;
            }

            @Override
            public int hashCode() {
                int result = questionId != null ? questionId.hashCode() : 0;
                result = 31 * result + (answerId != null ? answerId.hashCode() : 0);
                result = 31 * result + (checkboxIds != null ? checkboxIds.hashCode() : 0);
                return result;
            }

            public String getQuestionId() {
                return questionId;
            }

            public String getAnswerId() {
                return answerId;
            }

            public List<String> getCheckboxIds() {
                return checkboxIds;
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(final Parcel dest, int flags) {
                dest.writeString(getQuestionId());
                dest.writeString(getAnswerId());

                dest.writeStringList(checkboxIds);
            }
        }
    }
}
