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

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by murrayc on 8/28/14.
 */
public class BaseQuestionFragment extends ItemFragment {
    public static final String ARG_QUESTION_ID = "question-id";
    private String mQuestionId = null;
    private String mGroupId = null;

    String getGroupId() {
        return mGroupId;
    } //TODO: Should this be a long?

    void setGroupId(final String groupId) {
        mGroupId = groupId;
    }

    String getQuestionId() {
        return mQuestionId;
    }

    void setQuestionId(final String questionId) {
        mQuestionId = questionId;
    }

    DecisionTree.Question getQuestion() {
        final DecisionTree tree = getDecisionTree();
        if (tree == null) {
            Log.error("getQuestion(): tree is null.");
            return null;
        }

        final DecisionTree.Question question = tree.getQuestionOrFirst(getQuestionId());
        if (question != null) {
            //TODO: Is this useful/necessary?
            setQuestionId(question.getId());
        }

        return question;
    }

    DecisionTree getDecisionTree() {
        final Singleton singleton = getSingleton();
        return singleton.getDecisionTree(getGroupId());
    }

    BitmapDrawable getIcon(final Context context, final DecisionTree.BaseButton answer) {
        final Singleton singleton = getSingleton();
        return singleton.getIconDrawable(context, answer);
    }
}
