package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class QuestionHelpFragment extends BaseQuestionFragment {
    private View mRootView;

    public QuestionHelpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle != null) {
            setQuestionId(bundle.getString(QuestionFragment.ARG_QUESTION_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_question_help, container, false);

        //Do most of the UI building only after we know that our
        //Singleton has been initialized:
        initializeSingleton();

        return mRootView;
    }

    @Override
    void onSingletonInitialized() {
        super.onSingletonInitialized();

        update();
    }

    private void update() {
        // Use the Builder class for convenient dialog construction
        final Activity activity = getActivity();
        final DecisionTree.Question question = getQuestion();

        //Show the Help text:
        final TextView textView = (TextView) mRootView.findViewById(R.id.textView);
        if (textView == null) {
            Log.error("textView is null.");
            return;
        }
        textView.setText(question.getHelp());

        //Show the example images:
        final TableLayout tableLayout = (TableLayout) mRootView.findViewById(R.id.tableLayout);
        if (tableLayout == null) {
            Log.error("tableLayout is null.");
            return;
        }

        tableLayout.removeAllViews();
        for (final DecisionTree.Answer answer : question.answers) {
            addRowForAnswer(activity, tableLayout, question, answer);
        }

        for (final DecisionTree.Checkbox checkbox : question.checkboxes) {
            addRowForAnswer(activity, tableLayout, question, checkbox);
        }
    }

    private void addRowForAnswer(Activity activity, TableLayout tableLayout, DecisionTree.Question question, DecisionTree.BaseButton answer) {
        final TableRow row = new TableRow(activity);
        final TableLayout.LayoutParams params = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, UiUtils.getPxForDpResource(activity, R.dimen.standard_large_margin), 0, 0);
        tableLayout.addView(row, params);


        final LinearLayout layoutVertical = new LinearLayout(activity);
        layoutVertical.setOrientation(LinearLayout.VERTICAL);

        final TextView textViewAnswer = new TextView(activity);
        textViewAnswer.setText(answer.getText());
        layoutVertical.addView(textViewAnswer,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final LinearLayout layoutHorizontal = new LinearLayout(activity);
        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams paramsHorizontal = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsHorizontal.setMargins(0, UiUtils.getPxForDpResource(activity, R.dimen.standard_margin), 0, 0);
        layoutVertical.addView(layoutHorizontal, paramsHorizontal);

        final BitmapDrawable icon = getIcon(activity, answer);
        final ImageView imageIcon = new ImageView(activity);
        imageIcon.setImageDrawable(icon);
        layoutHorizontal.addView(imageIcon);

        // TODO: Use a custom FlowTable class to avoid items going off the right edge of the screen
        // when there are too many.
        final Singleton singleton = getSingleton();
        for (int i = 0; i < answer.getExamplesCount(); i++) {

            final String iconName = answer.getExampleIconName(question.getId(), i);
            final BitmapDrawable iconExample = singleton.getIconDrawable(activity, iconName);
            final ImageButton imageExample = new ImageButton(activity);
            //Remove the space between the image and the outside of the button:
            imageExample.setPadding(0, 0, 0, 0);
            imageExample.setImageDrawable(iconExample);

            //Needed to make the image expand as a transition into the SubjectViewerActivity,
            //which uses the same name in fragment_subject.xml
            ViewCompat.setTransitionName(imageExample, SubjectFragment.TRANSITION_NAME_SUBJECT_IMAGE);

            final LinearLayout.LayoutParams paramsImage = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            //This requires API level 17: paramsImage.setMarginStart(getPxForDp(activity, MARGIN_MEDIUM_DP));
            //imageExample.setLayoutParams(paramsImage);
            MarginLayoutParamsCompat.setMarginStart(paramsImage, UiUtils.getPxForDpResource(activity, R.dimen.standard_large_margin));

            imageExample.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    onExampleImageClicked(v, iconName);
                }
            });

            layoutHorizontal.addView(imageExample, paramsImage);
        }

        row.addView(layoutVertical,
                new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
    }

    private void onExampleImageClicked(final View imageButton, final String iconName) {
        //These images are not cached,
        //so we will need a network connection.
        final Activity activity = getActivity();
        if (!Utils.getNetworkIsConnected(activity)) {
            UiUtils.warnAboutNoNetworkConnection(activity);
            return;
        }

        final String uri = Config.FULL_EXAMPLE_URI + iconName + ".jpg";

        try {
            final Intent intent = new Intent(activity, ExampleViewerActivity.class);
            intent.putExtra(ExampleViewerFragment.ARG_EXAMPLE_URL, uri);

            //"subjectImageTransition" is also specified as transitionName="subjectImageTransition"
            //on the ImageButton here (when we created it) and in fragment_subject.xml.
            //TODO: Why do we need to specify it again here?
            final ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity, imageButton, SubjectFragment.TRANSITION_NAME_SUBJECT_IMAGE);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } catch (final ActivityNotFoundException e) {
            Log.error("Could not open the example image URI.", e);
        }
    }
}
