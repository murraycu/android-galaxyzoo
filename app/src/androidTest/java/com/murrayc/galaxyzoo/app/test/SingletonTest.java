/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-glom
 *
 * android-glom is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-glom is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-glom.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.murrayc.galaxyzoo.app.DecisionTree;
import com.murrayc.galaxyzoo.app.Singleton;
import com.murrayc.galaxyzoo.app.provider.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;

/**
 * Simple test to ensure that the generated bindings are working.
 */
@RunWith(AndroidJUnit4.class)
public class SingletonTest {
    private Context mockContext;

    @Before
    public void setup() {
        mockContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private Context getContext() {
        return mockContext;
    }

    private Singleton getSingleton() throws DecisionTree.DecisionTreeException {
        //TODO: Do this and wait for it?
        /*
        Singleton.init(getContext(),  new Singleton.Callbacks() {
            @Override
            public void onInitialized() {
                //onSingletonInitialized();
            }
        });
        */

        return new Singleton(getContext());
    }

    private DecisionTree getDecisionTreeSloan() throws DecisionTree.DecisionTreeException {
        final Singleton singleton = getSingleton();
        return singleton.getDecisionTree(Config.SUBJECT_GROUP_ID_SLOAN);
    }

    @Test
    public void testMultipleTrees() throws DecisionTree.DecisionTreeException, IOException {
        final Singleton singleton = new Singleton(getContext());

        final DecisionTree decisionTreeSloan = singleton.getDecisionTree(Config.SUBJECT_GROUP_ID_SLOAN);
        assertNotNull(decisionTreeSloan);
        assertNotNull(decisionTreeSloan.getAllQuestions());
        assertEquals(12, decisionTreeSloan.getAllQuestions().size());

        final DecisionTree decisionTreeDecals = singleton.getDecisionTree(Config.SUBJECT_GROUP_ID_DECALS);
        assertNotNull(decisionTreeDecals);
        assertNotNull(decisionTreeDecals.getAllQuestions());
        assertEquals(12, decisionTreeDecals.getAllQuestions().size());

        final DecisionTree decisionTreeIllustris = singleton.getDecisionTree(Config.SUBJECT_GROUP_ID_ILLUSTRIS);
        assertNotNull(decisionTreeIllustris);
        assertNotNull(decisionTreeIllustris.getAllQuestions());
        assertEquals(12, decisionTreeIllustris.getAllQuestions().size());

        assertNotSame(decisionTreeSloan, decisionTreeDecals);
        assertNotSame(decisionTreeSloan, decisionTreeIllustris);
        assertNotSame(decisionTreeDecals, decisionTreeIllustris);
    }

    public void testQuestionsWithoutTranslation() throws DecisionTree.DecisionTreeException, IOException {
        final DecisionTree decisionTree = getDecisionTreeSloan();

        final String QUESTION_ID = "sloan-3";
        final DecisionTree.Question question = decisionTree.getQuestion(QUESTION_ID);
        assertNotNull(question);
        assertEquals(QUESTION_ID, question.getId());

        assertEquals("Spiral", question.getTitle());
        assertEquals("Is there any sign of a spiral arm pattern?", question.getText());

        final DecisionTree.Question nextQuestion = decisionTree.getNextQuestionForAnswer(QUESTION_ID, "a-1");
        assertNotNull(nextQuestion);
        assertEquals("sloan-4", nextQuestion.getId());

        final List<DecisionTree.Answer> answers = question.getAnswers();
        assertNotNull(answers);
        DecisionTree.Answer answer = answers.get(0);
        assertNotNull(answer);
        assertEquals("Spiral", answer.getText());
        answer = answers.get(1);
        assertNotNull(answer);
        assertEquals("No spiral", answer.getText());

        checkAnswersForQuestionSloan4(question);
    }

    private static void checkAnswersForQuestionSloan4(final DecisionTree.Question question) {
        final List<DecisionTree.Answer> answers = question.getAnswers();
        assertNotNull(answers);
        assertEquals(2, answers.size());

        final DecisionTree.Answer answer = answers.get(0);
        assertNotNull(answer);
        assertEquals("a-0", answer.getId());
        assertEquals("yes", answer.getIcon());
        assertEquals(2, answer.getExamplesCount());
    }
}
