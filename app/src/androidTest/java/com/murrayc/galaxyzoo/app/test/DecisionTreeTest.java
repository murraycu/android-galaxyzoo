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
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class DecisionTreeTest extends AndroidTestCase {
    @Override
    public void setUp() {
    }

    private static DecisionTree createCorrectDecisionTree(final boolean withTranslation) throws DecisionTree.DecisionTreeException, IOException {
        //For some reason DecisionTreeTest.class.getResourceAsStream() doesn't work,
        //so we use DecisionTreeTest.class.getClassLoader().getResourceAsStream(), which does.
        final InputStream inputStreamDecisionTree = DecisionTreeTest.class.getClassLoader().getResourceAsStream("test_decision_tree.xml");
        assertNotNull(inputStreamDecisionTree);

        InputStream inputStreamTranslation = null;
        if (withTranslation) {
            inputStreamTranslation = DecisionTreeTest.class.getClassLoader().getResourceAsStream("test_translation.json");
            assertNotNull(inputStreamTranslation);
        }

        final DecisionTree decisionTree = new DecisionTree(inputStreamDecisionTree, inputStreamTranslation);

        if (inputStreamTranslation != null) {
            inputStreamTranslation.close();
        }

        inputStreamDecisionTree.close();

        return decisionTree;
    }

    @Override
    public void tearDown() {
    }

    public void testSize() throws DecisionTree.DecisionTreeException, IOException {
        final DecisionTree decisionTree = createCorrectDecisionTree(false);

        assertNotNull(decisionTree);
        assertNotNull(decisionTree.getAllQuestions());
        assertEquals(12, decisionTree.getAllQuestions().size());
    }

    public void testAllDecisionTreesWithAllTranslations() throws DecisionTree.DecisionTreeException, IOException {
        for (final Map.Entry<String, Config.SubjectGroup> entry : Config.SUBJECT_GROUPS.entrySet()) {
            final Config.SubjectGroup subjectGroup = entry.getValue();
            final String decisionTreeFilename = subjectGroup.getFilename();

            {
                final InputStream inputStreamTree = Utils.openAsset(getContext(),
                        Utils.getDecisionTreeFilepath(decisionTreeFilename));
                assertNotNull(inputStreamTree);

                //Test without a translation:
                final DecisionTree decisionTree = new DecisionTree(inputStreamTree, null);
                assertNotNull(decisionTree);
                assertNotNull(decisionTree.getAllQuestions());

                inputStreamTree.close();
            }

            //Test with all translations:
            //TODO: Get them all automatically.
            final String[] locales = {"de", "fr", "it"};
            for (final String locale : locales) {
                final InputStream inputStreamTree = Utils.openAsset(getContext(),
                        Utils.getDecisionTreeFilepath(decisionTreeFilename));
                assertNotNull(inputStreamTree);

                final InputStream inputStreamTranslation = Utils.openAsset(getContext(),
                        Utils.getTranslationFilePath(locale, null));
                assertNotNull(inputStreamTranslation);

                final DecisionTree decisionTree = new DecisionTree(inputStreamTree, null);
                assertNotNull(decisionTree);
                assertNotNull(decisionTree.getAllQuestions());

                inputStreamTranslation.close();
                inputStreamTree.close();

            }
        }
    }

    public void testQuestionsWithTranslation() throws DecisionTree.DecisionTreeException, IOException {
        final DecisionTree decisionTree = createCorrectDecisionTree(true /* withTranslation */);

        final String QUESTION_ID = "sloan-3";
        final DecisionTree.Question question = decisionTree.getQuestion(QUESTION_ID);
        assertEquals(QUESTION_ID, question.getId());

        //The French translation, because we are using a translation .json file:
        assertEquals("Spirale", question.getTitle());
        assertEquals("Est-ce qu’il y a un signe de motif de bras spiral ?", question.getText());

        final DecisionTree.Question nextQuestion = decisionTree.getNextQuestionForAnswer(QUESTION_ID, "a-1");
        assertEquals("sloan-4", nextQuestion.getId());

        final List<DecisionTree.Answer> answers = question.getAnswers();
        assertNotNull(answers);
        DecisionTree.Answer answer = answers.get(0);
        assertNotNull(answer);
        assertEquals("Spirale", answer.getText());
        answer = answers.get(1);
        assertNotNull(answer);
        assertEquals("Pas de spirale", answer.getText());

        checkAnswersForQuestionSloan4(question);
    }

    public void testQuestionsWithoutTranslation() throws DecisionTree.DecisionTreeException, IOException {
        final DecisionTree decisionTree = createCorrectDecisionTree(false /* withTranslation */);

        final String QUESTION_ID = "sloan-3";
        final DecisionTree.Question question = decisionTree.getQuestion(QUESTION_ID);
        assertEquals(QUESTION_ID, question.getId());

        assertEquals("Spiral", question.getTitle());
        assertEquals("Is there any sign of a spiral arm pattern?", question.getText());

        final DecisionTree.Question nextQuestion = decisionTree.getNextQuestionForAnswer(QUESTION_ID, "a-1");
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
