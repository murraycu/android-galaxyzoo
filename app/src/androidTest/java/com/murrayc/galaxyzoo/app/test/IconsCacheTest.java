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

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

import com.murrayc.galaxyzoo.app.DecisionTree;
import com.murrayc.galaxyzoo.app.IconsCache;
import com.murrayc.galaxyzoo.app.Config;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class IconsCacheTest extends AndroidTestCase {
    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
    }

    static void checkIcon(final IconsCache iconsCache, final String iconName) {
        assertNotNull(iconName);
        assertFalse(iconName.isEmpty());

        final Bitmap exampleBitmap = iconsCache.getIcon(iconName);
        //Log.info("checking iconName=" + iconName);
        assertNotNull("getIcon() returned null for: " + iconName, exampleBitmap);

        assertEquals(Config.ICON_WIDTH_HEIGHT, exampleBitmap.getHeight());
        assertEquals(Config.ICON_WIDTH_HEIGHT, exampleBitmap.getWidth());
    }

    public void testIconsCache() throws DecisionTree.DecisionTreeException, IOException {
        final List<DecisionTree> trees = new ArrayList<>();
        for (final Map.Entry<String, com.murrayc.galaxyzoo.app.provider.Config.SubjectGroup> entry : com.murrayc.galaxyzoo.app.provider.Config.SUBJECT_GROUPS.entrySet()) {
            final com.murrayc.galaxyzoo.app.provider.Config.SubjectGroup subjectGroup = entry.getValue();
            final String decisionTreeFilename = subjectGroup.getFilename();
            final InputStream inputStreamTree = Utils.openAsset(getContext(),
                    Utils.getDecisionTreeFilepath(decisionTreeFilename));
            assertNotNull(inputStreamTree);

            final DecisionTree decisionTree = new DecisionTree(inputStreamTree, null);
            assertNotNull(decisionTree);
            assertNotNull(decisionTree.getAllQuestions());

            trees.add(decisionTree);
        }

        final IconsCache iconsCache = new IconsCache(getContext(), trees);

        for (final DecisionTree decisionTree : trees) {
            for (final DecisionTree.Question question : decisionTree.getAllQuestions()) {
                assertNotNull(question);

                for (final DecisionTree.Answer answer : question.getAnswers()) {
                    checkAnswer(iconsCache, question, answer);
                }

                for (final DecisionTree.Checkbox answer : question.getCheckboxes()) {
                    checkAnswer(iconsCache, question, answer);
                }
            }
        }
    }

    private static boolean checkFileExistsAtUri(final String uri) throws IOException {
        final URL url = new URL(uri);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.connect();
        final boolean exists = con.getResponseCode() == HttpURLConnection.HTTP_OK;
        con.disconnect();
        return exists;
    }

    private static void checkAnswer(final IconsCache iconsCache, final DecisionTree.Question question, final DecisionTree.BaseButton answer) throws IOException {
        final String iconName = answer.getIcon();
        checkIcon(iconsCache, iconName);

        final int count = answer.getExamplesCount();
        for (int i = 0; i < count; ++i) {
            //Check that we have a thumbnail icon for the example image:
            final String exampleIconName = answer.getExampleIconName(question.getId(), i);
            checkIcon(iconsCache, exampleIconName);

            //Check that the full example actually exists on the server:
            final String exampleUri = IconsCache.getExampleImageUri(exampleIconName);
            assertTrue("Cannot get full example image:"  + exampleUri,
                    checkFileExistsAtUri(exampleUri));
            /*
            if (!checkFileExistsAtUri(exampleUri)) {
                final String exampleIconName2 = answer.getExampleIconNameWithCommonMistake(question.getId(), i);
                final String exampleUri2 = IconsCache.getExampleImageUri(exampleIconName2);
                assertTrue("Cannot get full example image even by checking for a common mistake in the URL:"  + exampleUri,
                        checkFileExistsAtUri(exampleUri2));
            }
            */
        }
    }


}
