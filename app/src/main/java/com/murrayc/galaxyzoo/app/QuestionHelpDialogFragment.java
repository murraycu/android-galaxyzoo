package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.murrayc.galaxyzoo.app.provider.Config;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QuestionHelpDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class QuestionHelpDialogFragment extends DialogFragment {
    private Singleton mSingleton;
    private String mQuestionId;
    private View mRootView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param questionId The question ID
     * @return A new instance of fragment HelpDialogFragment.
     */
    public static QuestionHelpDialogFragment newInstance(final String questionId) {
        QuestionHelpDialogFragment fragment = new QuestionHelpDialogFragment();
        Bundle args = new Bundle();
        args.putString(QuestionFragment.ARG_QUESTION_ID, questionId);
        fragment.setArguments(args);
        return fragment;
    }

    public QuestionHelpDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        if (bundle != null) {
            setQuestionId(bundle.getString(QuestionFragment.ARG_QUESTION_ID));
        }

        final Activity activity = getActivity();

        final LayoutInflater inflater = activity.getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());
        mRootView = inflater.inflate(R.layout.fragment_question_help_dialog, null);
        builder.setView(mRootView);

        builder.setTitle("Help");
        builder.setPositiveButton("Close", null);

        //Do most of the UI building only after we know that our
        //Singleton has been initialized:
        Singleton.init(activity, new Singleton.Callbacks() {
            @Override
            public void onInitialized() {
                QuestionHelpDialogFragment.this.mSingleton = Singleton.getInstance();

                update();
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void update() {
        // Use the Builder class for convenient dialog construction
        final Activity activity = getActivity();
        final DecisionTree.Question question = getQuestion();

        //Show the Help text:
        final TextView textView = (TextView)mRootView.findViewById(R.id.textView);
        if (textView == null) {
            Log.error("textView is null.");
            return;
        }
        textView.setText(question.getHelp());

        //Show the example images:
        final TableLayout tableLayout = (TableLayout)mRootView.findViewById(R.id.tableLayout);
        if (tableLayout == null) {
            Log.error("tableLayout is null.");
            return;
        }

        tableLayout.removeAllViews();
        for(final DecisionTree.Answer answer : question.answers) {
            addRowForAnswer(activity, tableLayout, question, answer);
        }

        tableLayout.removeAllViews();
        for(final DecisionTree.Checkbox checkbox : question.checkboxes) {
            addRowForAnswer(activity, tableLayout, question, checkbox);
        }
    }

    private void addRowForAnswer(Activity activity, TableLayout tableLayout, DecisionTree.Question question, DecisionTree.BaseButton answer) {
        final TableRow row = new TableRow(activity);
        row.setLayoutParams(
                new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.addView(row,
                new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));
        //TODO: Add padding between the rows.

        final LinearLayout layoutVertical = new LinearLayout(activity);
        layoutVertical.setOrientation(LinearLayout.VERTICAL);

        final TextView textViewAnswer = new TextView(activity);
        textViewAnswer.setText(answer.getText());
        layoutVertical.addView(textViewAnswer);

        final LinearLayout layoutHorizontal = new LinearLayout(activity);
        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        layoutVertical.addView(layoutHorizontal);

        final BitmapDrawable icon = getIcon(activity, answer);
        final ImageView imageIcon = new ImageView(activity);
        imageIcon.setImageDrawable(icon);
        layoutHorizontal.addView(imageIcon);

        final Singleton singleton = getSingleton();
        for (int i = 0; i < answer.getExamplesCount(); i++) {

            final String iconName = answer.getExampleIconName(question.getId(), i);
            final BitmapDrawable iconExample = singleton.getIconDrawable(activity, iconName);
            final ImageButton imageExample = new ImageButton(activity);
            imageExample.setImageDrawable(iconExample);

            imageExample.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    onExampleImageClicked(iconName);
                }
            });

            layoutHorizontal.addView(imageExample);

            //TODO: Show the full image on click.
        }

        row.addView(layoutVertical);
    }

    private void onExampleImageClicked(final String iconName) {
        final String uri = Config.FULL_EXAMPLE_URI + iconName + ".jpg";

        //TODO: Show the image within the app instead of in the browser:
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        } catch (final ActivityNotFoundException e) {
            Log.error("Could not open the example image URI.", e);
        }
    }

    public Singleton getSingleton() {
        return mSingleton;
    }


    private DecisionTree.Question getQuestion() {
        final Singleton singleton = getSingleton();
        final DecisionTree tree = singleton.getDecisionTree();

        DecisionTree.Question question = tree.getQuestionOrFirst(getQuestionId());
        setQuestionId(question.getId());
        return question;
    }

    private void setQuestionId(final String questionId) {
        mQuestionId = questionId;
    }

    private BitmapDrawable getIcon(final Context context, final DecisionTree.BaseButton answer) {
        final Singleton singleton = getSingleton();
        return singleton.getIconDrawable(context, answer);
    }

    public String getQuestionId() {
        return mQuestionId;
    }
}
