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

import android.test.AndroidTestCase;

import com.murrayc.galaxyzoo.app.DecisionTree;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class DecisionTreeTest extends AndroidTestCase {
    private static DecisionTree decisionTree;


    @Override
    public void setUp() throws IOException {
        //For some reason DecisionTreeTest.class.getResourceAsStream() doesn't work,
        //so we use DecisionTreeTest.class.getClassLoader().getResourceAsStream(), which does.
        final InputStream inputStream = DecisionTreeTest.class.getClassLoader().getResourceAsStream("test_decision_tree.xml");
        assertNotNull(inputStream);

        //TODO: Close the stream.
        decisionTree = new DecisionTree(inputStream);

        inputStream.close();
    }

    @Override
    public void tearDown() {
    }

    public void testSize() {
        assertNotNull(decisionTree);
        assertNotNull(decisionTree.questionsMap);
        assertEquals(12, decisionTree.questionsMap.size());
    }

    public void testQuestions() {
        final String QUESTION_ID = "sloan-3";
        final DecisionTree.Question question = decisionTree.getQuestion(QUESTION_ID);
        assertEquals(QUESTION_ID, question.getId());
        assertEquals("Spiral", question.getTitle());
        assertEquals("Is there any sign of a spiral arm pattern?", question.getText());

        final DecisionTree.Question nextQuestion = decisionTree.getNextQuestionForAnswer(QUESTION_ID, "a-1");
        assertEquals("sloan-4", nextQuestion.getId());

        //TODO: Test getQuestion() and getNextQuestion().
    }

}
