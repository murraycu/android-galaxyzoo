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

import android.os.Parcel;

import com.murrayc.galaxyzoo.app.QuestionFragment;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 */
public class ClassificationInProgressTest {
    @Test
    public void testParcelable() {
        final QuestionFragment.ClassificationInProgress classificationInProgress =
                createTestClassificationInProgress();

        // Obtain a Parcel object and write the parcelable object to it:
        final Parcel parcel = Parcel.obtain();
        classificationInProgress.writeToParcel(parcel, 0);

        // After you're done with writing, you need to reset the parcel for reading:
        parcel.setDataPosition(0);

        // Reconstruct object from parcel and asserts:
        final QuestionFragment.ClassificationInProgress createdFromParcel = QuestionFragment.ClassificationInProgress.CREATOR.createFromParcel(parcel);
        //noinspection ObjectEquality
        assertFalse("Parcel is the same.", classificationInProgress == createdFromParcel);
        assertEquals(classificationInProgress, createdFromParcel);
    }

    private static QuestionFragment.ClassificationInProgress createTestClassificationInProgress() {
        final QuestionFragment.ClassificationInProgress result = new QuestionFragment.ClassificationInProgress();
        result.add("testQuestionId1", "testAnswerId1.1", null);
        result.add("testQuestionId2", "testAnswerId1.2", null);

        final List<String> checkboxIds = new ArrayList<>();
        checkboxIds.add("testCheckboxId1");
        checkboxIds.add("testCheckboxId2");
        result.add("testQuestionId3", "testAnswerId3.1", checkboxIds);
        result.add("testQuestionId3", "testAnswerId3.2", null);

        result.add("testQuestionId4", "testAnswerId4.1", null);

        return result;
    }

    @Test
    public void testEqualsExpectSuccess() {
        final QuestionFragment.ClassificationInProgress a = createTestClassificationInProgress();
        final QuestionFragment.ClassificationInProgress b = createTestClassificationInProgress();
        assertEquals(a, b);
        assertEquals(b, a);

    }


    public void testEqualsExpectFailureExtraAnswer() {
        final QuestionFragment.ClassificationInProgress a = createTestClassificationInProgress();
        final QuestionFragment.ClassificationInProgress b = createTestClassificationInProgress();
        b.add("testQuestionId4", "testAnswerId4.1", null);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }

    @Test
    public void testEqualsExpectFailureExtraQuestion() {
        final QuestionFragment.ClassificationInProgress a = createTestClassificationInProgress();
        final QuestionFragment.ClassificationInProgress b = createTestClassificationInProgress();
        b.add("testQuestionId5", "testAnswerId5.1", null);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }
}
