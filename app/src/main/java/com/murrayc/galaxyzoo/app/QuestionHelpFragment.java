/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatTextView;
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
    public static final String ARG_GROUP_ID = "group-id";
    private View mRootView = null;

    public QuestionHelpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle != null) {
            setQuestionId(bundle.getString(QuestionFragment.ARG_QUESTION_ID));
            setGroupId(bundle.getString(ARG_GROUP_ID));
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
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

    public void update() {
        // Use the Builder class for convenient dialog construction
        final Context context = getActivity();
        final DecisionTree.Question question = getQuestion();
        if (question == null) {
            Log.error("update(): question is null.");
            return;
        }

        //Show the Help text:
        final TextView textView = (TextView) mRootView.findViewById(R.id.textView);
        if (textView == null) {
            Log.error("update(): textView is null.");
            return;
        }
        textView.setText(question.getHelp());

        //Show the example images:
        final TableLayout tableLayout = (TableLayout) mRootView.findViewById(R.id.tableLayout);
        if (tableLayout == null) {
            Log.error("update(): tableLayout is null.");
            return;
        }

        tableLayout.removeAllViews();
        for (final DecisionTree.Answer answer : question.getAnswers()) {
            addRowForAnswer(context, tableLayout, question, answer);
        }

        for (final DecisionTree.Checkbox checkbox : question.getCheckboxes()) {
            addRowForAnswer(context, tableLayout, question, checkbox);
        }
    }

    private void addRowForAnswer(final Context context, final TableLayout tableLayout, final DecisionTree.Question question, final DecisionTree.BaseButton answer) {
        final TableRow row = new TableRow(context);
        final TableLayout.LayoutParams params = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, UiUtils.getPxForDpResource(context, R.dimen.standard_large_margin), 0, 0);
        tableLayout.addView(row, params);


        final LinearLayout layoutVertical = new LinearLayout(context);
        layoutVertical.setOrientation(LinearLayout.VERTICAL);

        final TextView textViewAnswer = new AppCompatTextView(context);
        textViewAnswer.setTextAppearance(context, R.style.TextAppearance_AppCompat_Subhead);
        textViewAnswer.setText(answer.getText());
        layoutVertical.addView(textViewAnswer,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final LinearLayout layoutHorizontal = new LinearLayout(context);
        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams paramsHorizontal = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsHorizontal.setMargins(0, UiUtils.getPxForDpResource(context, R.dimen.standard_margin), 0, 0);
        layoutVertical.addView(layoutHorizontal, paramsHorizontal);

        final BitmapDrawable icon = getIcon(context, answer);
        final ImageView imageIcon = new ImageView(context);
        imageIcon.setImageDrawable(icon);
        layoutHorizontal.addView(imageIcon);

        final LinearLayout.LayoutParams paramsImage = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // TODO: Use a custom FlowTable class to avoid items going off the right edge of the screen
        // when there are too many.
        final Singleton singleton = getSingleton();
        for (int i = 0; i < answer.getExamplesCount(); i++) {

            final String iconName = answer.getExampleIconName(question.getId(), i);
            final BitmapDrawable iconExample = singleton.getIconDrawable(context, iconName);
            final ImageButton imageExample = new ImageButton(context);
            //Remove the space between the image and the outside of the button:
            imageExample.setPadding(0, 0, 0, 0);
            imageExample.setImageDrawable(iconExample);

            //Needed to make the image expand as a transition into the SubjectViewerActivity,
            //which uses the same name in fragment_subject.xml
            ViewCompat.setTransitionName(imageExample, getString(R.string.transition_subject_image));

            //This requires API level 17: paramsImage.setMarginStart(getPxForDp(activity, MARGIN_MEDIUM_DP));
            //imageExample.setLayoutParams(paramsImage);
            MarginLayoutParamsCompat.setMarginStart(paramsImage, UiUtils.getPxForDpResource(context, R.dimen.standard_large_margin));

            final int answerIndex = i;
            imageExample.setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {
                    // Perform action on click
                    onExampleImageClicked(v, answer, answerIndex);
                }
            });

            layoutHorizontal.addView(imageExample, paramsImage);
        }

        row.addView(layoutVertical,
                new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
    }

    private void onExampleImageClicked(final View imageButton, final DecisionTree.BaseButton answer, final int answerIndex) {
        //These images are not cached,
        //so we will need a network connection.
        final Activity activity = getActivity();
        final boolean requireWiFi = false; //This is an explicit request. But TODO: Ask for confirmation if wifi-only is on.
        if(UiUtils.warnAboutMissingNetwork(activity, requireWiFi)) {
            return;
        }

        final String questionId = getQuestionId();
        final String iconName = answer.getExampleIconName(questionId, answerIndex);
        final String uri = IconsCache.getExampleImageUri(iconName);

        final String iconNameAlternative = answer.getExampleIconNameWithCommonMistake(questionId, answerIndex);
        final String uriAlternative = IconsCache.getExampleImageUri(iconNameAlternative);

        final Intent intent = new Intent(activity, ExampleViewerActivity.class);
        intent.putExtra(ExampleViewerFragment.ARG_EXAMPLE_URL, uri);
        intent.putExtra(ExampleViewerFragment.ARG_EXAMPLE_URL_ALTERNATIVE, uriAlternative);


        //"subjectImageTransition" is also specified as transitionName="subjectImageTransition"
        //on the ImageButton here (when we created it) and in fragment_subject.xml.
        //TODO: Why do we need to specify it again here?
        ActivityCompat.startActivity(activity, intent, UiUtils.getTransitionOptionsBundle(activity, imageButton));
    }

}
