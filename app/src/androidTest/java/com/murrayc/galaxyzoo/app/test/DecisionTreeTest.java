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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class DecisionTreeTest extends AndroidTestCase {
    @Override
    public void setUp() {
    }

    private static DecisionTree createCorrectDecisionTree() throws DecisionTree.DecisionTreeException, IOException {
        //For some reason DecisionTreeTest.class.getResourceAsStream() doesn't work,
        //so we use DecisionTreeTest.class.getClassLoader().getResourceAsStream(), which does.
        final InputStream inputStream = DecisionTreeTest.class.getClassLoader().getResourceAsStream("test_decision_tree.xml");
        assertNotNull(inputStream);

        //TODO: Close the stream.
        final DecisionTree decisionTree = new DecisionTree(inputStream, null);

        inputStream.close();

        return decisionTree;
    }

    @Override
    public void tearDown() {
    }

    public void testSize() throws DecisionTree.DecisionTreeException, IOException {
        final DecisionTree decisionTree = createCorrectDecisionTree();

        assertNotNull(decisionTree);
        assertNotNull(decisionTree.questionsMap);
        assertEquals(12, decisionTree.questionsMap.size());
    }

    public void testQuestions() throws DecisionTree.DecisionTreeException, IOException {
        final DecisionTree decisionTree = createCorrectDecisionTree();

        final String QUESTION_ID = "sloan-3";
        final DecisionTree.Question question = decisionTree.getQuestion(QUESTION_ID);
        assertEquals(QUESTION_ID, question.getId());
        assertEquals("Spiral", question.getTitle());
        assertEquals("Is there any sign of a spiral arm pattern?", question.getText());

        final DecisionTree.Question nextQuestion = decisionTree.getNextQuestionForAnswer(QUESTION_ID, "a-1");
        assertEquals("sloan-4", nextQuestion.getId());

        //TODO: Test getQuestion() and getNextQuestion().
    }

    public void testParseBadXml() {
        final String xml = "nonsense";
        final InputStream is = new ByteArrayInputStream(xml.getBytes());

        //This should throw an exception:
        final DecisionTree decisionTree;
        try {
            decisionTree = new DecisionTree(is, null);
            assertNull(decisionTree); //This should not be reached.
        } catch (final DecisionTree.DecisionTreeException e) {
            assertNotNull(e); //just to avoid an empty catch block.
        }
    }

}
