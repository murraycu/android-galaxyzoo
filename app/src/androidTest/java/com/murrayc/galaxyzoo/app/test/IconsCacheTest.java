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
import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.Utils;

import java.io.IOException;
import java.io.InputStream;
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

    void checkIcon(final IconsCache iconsCache, final String iconName) {
        assertNotNull(iconName);
        assertFalse(iconName.isEmpty());

        final Bitmap exampleBitmap = iconsCache.getIcon(iconName);
        assertNotNull(exampleBitmap);

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

    private void checkAnswer(IconsCache iconsCache, DecisionTree.Question question, DecisionTree.BaseButton answer) {
        final int count = answer.getExamplesCount();
        for (int i = 0; i < count; ++i) {
            final String iconName = answer.getIcon();
            checkIcon(iconsCache, iconName);

            final String exampleIconName = answer.getExampleIconName(question.getId(), i);
            checkIcon(iconsCache, exampleIconName);
        }
    }


}
