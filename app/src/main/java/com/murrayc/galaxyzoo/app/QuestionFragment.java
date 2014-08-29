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
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.murrayc.galaxyzoo.app.provider.ClassificationAnswer;
import com.murrayc.galaxyzoo.app.provider.ClassificationCheckbox;
import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment representing a single subject.
 * This fragment is either contained in a {@link com.murrayc.galaxyzoo.app.ListActivity}
 * in two-pane mode (on tablets) or a {@link com.murrayc.galaxyzoo.app.ClassifyActivity}
 * on handsets.
 */
public class QuestionFragment extends BaseQuestionFragment
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String ARG_QUESTION_CLASSIFICATION_IN_PROGRESS = "classification-in-progress";

    private static final int URL_LOADER = 0;
    private Cursor mCursor;

    private final String[] mColumns = { Item.Columns._ID, Item.Columns.ZOONIVERSE_ID };

    // We have to hard-code the indices - we can't use getColumnIndex because the Cursor
    // (actually a SQliteDatabase cursor returned
    // from our ContentProvider) only knows about the underlying SQLite database column names,
    // not our ContentProvider's column names. That seems like a design error in the Android API.
    //TODO: Use org.apache.commons.lang.ArrayUtils.indexOf() instead?
    private static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_ZOONIVERSE_ID = 1;


    //We hard-code this.
    //Alternatively, we could hard-code the removal of this question from the XML
    //when generating the XML file,
    //and then always ask the question at the end via Java code.
    private static final CharSequence QUESTION_ID_DISCUSS = "sloan-11";
    private static final CharSequence ANSWER_ID_DISCUSS_YES = "a-0";
    private String mZooniverseId; //Only used for the talk URI so far.

    // A map of checkbox IDs to buttons.
    private Map<String, ToggleButton> mCheckboxButtons = new HashMap<>();
    private boolean mLoaderFinished = false;

    private void setZooniverseId(final String zooniverseId) {
        mZooniverseId = zooniverseId;
    }

    private String getZooniverseId() {
        return mZooniverseId;
    }


    /** This lets us store the classification's answers
     * during the classification. Alternatively,
     * we could insert the answers into the ContentProvider along the
     * way, but this lets us avoid having half-complete classifications
     * in the content provider.
     */
    static private class ClassificationInProgress implements Parcelable {
        public static final Parcelable.Creator<ClassificationInProgress> CREATOR
                = new Parcelable.Creator<ClassificationInProgress>() {
            public ClassificationInProgress createFromParcel(Parcel in) {
                return new ClassificationInProgress(in);
            }

            public ClassificationInProgress[] newArray(int size) {
                return new ClassificationInProgress[size];
            }
        };

        public ClassificationInProgress() {

        }

        public ClassificationInProgress(final Parcel in) {
            final Object[] array = in.readArray(String.class.getClassLoader());
            if ((array != null) && (array.length != 0)) {
                for (final Object object : array) {
                    final QuestionAnswer str = (QuestionAnswer)object;
                    this.answers.add(str);
                }
            }
        }

        public void add(final String questionId, final String answerId) {
            answers.add(new QuestionAnswer(questionId, answerId));
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
            dest.writeArray(answers.toArray());
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

            public QuestionAnswer(final String questionId, final String answerId) {
                this.questionId = questionId;
                this.answerId = answerId;
            }

            public QuestionAnswer(final String questionId, final String answerId, final List<String> checkboxIds) {
                this.questionId = questionId;
                this.answerId = answerId;
                this.checkboxIds = checkboxIds;
            }

            private QuestionAnswer(final Parcel in) {
                //Keep this in sync with writeToParcel().
                this.questionId = in.readString();
                this.answerId = in.readString();

                final Object[] array = in.readArray(String.class.getClassLoader());
                if ((array != null) && (array.length != 0)) {
                    this.checkboxIds = new ArrayList<>();
                    for (final Object object : array) {
                        final String str = (String)object;
                        this.checkboxIds.add(str);
                    }
                }
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

                if (checkboxIds != null) {
                    dest.writeArray(checkboxIds.toArray());
                }
            }
        }

        List<QuestionAnswer> getAnswers() {
            return answers;
        }

        private List<QuestionAnswer> answers = new ArrayList<>();
    }

    //TODO: Can this fragment be reused, meaning we'd need to reset this?
    private ClassificationInProgress mClassificationInProgress = new ClassificationInProgress();

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

        /** We call this when the classification has been finished and saved.
         */
        public void onClassificationFinished();
    }

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
        mRootView = inflater.inflate(R.layout.fragment_question, container, false);
        assert mRootView != null;

        setHasOptionsMenu(true);

        Singleton.init(getActivity(), new Singleton.Callbacks() {
            @Override
            public void onInitialized() {
                QuestionFragment.this.mSingleton = Singleton.getInstance();

                updateIfReady();
            }
        });

        //This will be called later by updateIfReady(): update();

        return mRootView;
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
        final MenuItem menuItem = menu.add(Menu.NONE, R.id.option_menu_item_examples, Menu.NONE, R.string.action_examples);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.option_menu_item_examples:
                doExamples();
                return true;
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


        if (mRootView == null) {
            Log.error("mRootView is null.");
            return;
        }

        final DecisionTree.Question question = getQuestion();

        //Show the title:
        final TextView textViewTitle = (TextView)mRootView.findViewById(R.id.textViewTitle);
        if (textViewTitle == null) {
            Log.error("textViewTitle is null.");
            return;
        }
        textViewTitle.setText(question.getTitle());

        //Show the text:
        final TextView textViewText = (TextView)mRootView.findViewById(R.id.textViewText);
        if (textViewText == null) {
            Log.error("textViewText is null.");
            return;
        }
        textViewText.setText(question.getText());


        final GridLayout layoutAnswers = (GridLayout)mRootView.findViewById(R.id.layoutAnswers);
        if (layoutAnswers == null) {
            Log.error("layoutAnswers is null.");
            return;
        }

        layoutAnswers.removeAllViews();

        //Checkboxes:
        mCheckboxButtons.clear();
        for(final DecisionTree.Checkbox checkbox : question.checkboxes) {
            final ToggleButton button = new ToggleButton(activity);
            makeButtonTextSmall(activity, button);

            //Use just the highlighting (line, color, etc) to show that it's selected,
            //instead of On/Off, so we don't need a separate label.
            //TODO: Use the icon. See http://stackoverflow.com/questions/18598255/android-create-a-toggle-button-with-image-and-no-text
            //TODO: Avoid the highlight bar thing at the bottom being drawn over the text.
            button.setText(checkbox.getText());
            button.setTextOn(checkbox.getText());
            button.setTextOff(checkbox.getText());

            //We specify Gravity.FILL to make the buttons all be the same width and height.
            //Note that just using button.setGravity() doesn't seem to work.
            final GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.setGravity(Gravity.FILL);
            layoutAnswers.addView(button, params);

            final BitmapDrawable icon = getIcon(activity, checkbox);
            button.setCompoundDrawables(null, icon, null, null);

            mCheckboxButtons.put(checkbox.getId(), button);

            /*
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    onAnswerButtonClicked(checkbox.getId());
                }
            });
            */
        }

        //Answers:
        for(final DecisionTree.Answer answer : question.answers) {
            final Button button = createAnswerButton(activity, answer);

            //We specify Gravity.FILL to make the buttons all be the same width and height.
            //Note that just using button.setGravity() doesn't seem to work.
            final GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.setGravity(Gravity.FILL);
            layoutAnswers.addView(button, params);

            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    onAnswerButtonClicked(question.getId(), answer.getId());
                }
            });
        }
    }

    private void makeButtonTextSmall(final Activity activity, final Button button) {
        button.setTextAppearance(activity, android.R.style.TextAppearance_Small);
    }

    private Button createAnswerButton(Activity activity, DecisionTree.Answer answer) {
        final Button button = new Button(activity);
        button.setText(answer.getText());
        makeButtonTextSmall(activity, button);

        final BitmapDrawable icon = getIcon(activity, answer);
        button.setCompoundDrawables(null, icon, null, null);
        return button;
    }

    private void onAnswerButtonClicked(final String questionId, final String answerId) {
        if (questionId == null) {
            Log.error("onAnswerButtonClicked: questionId was null.");
        }

        //TODO: Move this logic to the parent ClassifyFragment?
        final Activity activity = getActivity();
        if (activity == null)
            return;

        if((TextUtils.equals(questionId, QUESTION_ID_DISCUSS)) &&
                (TextUtils.equals(answerId, ANSWER_ID_DISCUSS_YES))) {
            //Open a link to the discussion page.

            //Todo: Find a way to use Uri.Builder with a URI with # in it.
            //Using Uri.parse() (with Uri.Builder) removes the #.
            //Using Uri.Builder() leads to an ActivityNotFoundException.
            //final String encodedHash = Uri.encode("#"); //This just puts %23 in the URL instead of #.
            //final Uri.Builder uriBuilder = new Uri.Builder();
            //uriBuilder.path("http://talk.galaxyzoo.org/#/subjects/");
            //uriBuilder.appendPath(getZooniverseId());
            final String uriTalk = Config.TALK_URI + getZooniverseId();

            try {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriTalk));
                startActivity(intent);
            } catch (final ActivityNotFoundException e) {
                Log.error("Could not open the discussion URI.", e);
            }
        } else {
            List<String> checkboxes = null;

            //Get the selected checkboxes too:
            final Singleton singleton = getSingleton();
            final DecisionTree tree = singleton.getDecisionTree();
            final DecisionTree.Question question = tree.getQuestion(questionId);
            if (question.hasCheckboxes()) {
                checkboxes = new ArrayList<>();
                for (final DecisionTree.Checkbox checkbox : question.checkboxes) {
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

        final Singleton singleton = getSingleton();
        final DecisionTree tree = singleton.getDecisionTree();

        final DecisionTree.Question question = tree.getNextQuestionForAnswer(questionId, answerId);
        if(question != null) {
            setQuestionId(question.getId());
            update();
        } else {
            //The classification is finished.
            //We save it to the ContentProvider, which will upload it.
            //TODO: Prevent the user from being able to press the button again between now
            //and the new subject being shown.
            saveClassification(mClassificationInProgress);
            mClassificationInProgress = new ClassificationInProgress();
            setQuestionId(null);

            //TODO: Do something else for tablet UIs that share the activity.
            mCallbacks.onClassificationFinished();
        }
    }

    private void saveClassification(final ClassificationInProgress classificationInProgress) {
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

        final String itemId = getItemId();
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
        final ContentValues valuesDone = new ContentValues();
        valuesDone.put(Item.Columns.DONE, true);
        builder.withValues(valuesDone);
        ops.add(builder.build());

        try {
            resolver.applyBatch(ClassificationAnswer.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        //The ItemsContentProvider will upload the classification later.
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
            Log.error("The ContentProvider query returned no rows.");

            //Check for this possible cause.
            // TODO: Is there any simpler way to just catch the
            // ItemsContentProvider.NoNetworkConnection exception in the CursorLoader?
            if (!Utils.getNetworkIsConnected(activity)) {
                UiUtils.warnAboutNoNetworkConnection(activity);
            }

            return;
        }

        if (mCursor.getColumnCount() <= 0) { //In case the query returned no columns.
            Log.error("The ContentProvider query returned no columns.");
            return;
        }

        mCursor.moveToFirst(); //There should only be one anyway.

        //Look at each group in the layout:
        if (mRootView == null) {
            Log.error("mRootView is null.");
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
}
