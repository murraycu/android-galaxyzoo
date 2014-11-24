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
import android.test.AndroidTestCase;

import com.murrayc.galaxyzoo.app.DecisionTree;
import com.murrayc.galaxyzoo.app.QuestionFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class ClassificationInProgressTest extends AndroidTestCase {


    @Override
    public void setUp() {

    }

    @Override
    public void tearDown() {
    }

    public void testParcelable() {
        final QuestionFragment.ClassificationInProgress classificationInProgress = new QuestionFragment.ClassificationInProgress();
        classificationInProgress.add("testQuestionId1", "testAnswerId1.1", null);
        classificationInProgress.add("testQuestionId2", "testAnswerId1.2", null);

        List<String> checkboxIds = new ArrayList<>();
        checkboxIds.add("testCheckboxId1");
        checkboxIds.add("testCheckboxId2");
        classificationInProgress.add("testQuestionId3", "testAnswerId3.1", checkboxIds);
        classificationInProgress.add("testQuestionId3", "testAnswerId3.2", null);

        classificationInProgress.add("testQuestionId4", "testAnswerId4.1", null);


        // Obtain a Parcel object and write the parcelable object to it:
        final Parcel parcel = Parcel.obtain();
        classificationInProgress.writeToParcel(parcel, 0);

        // After you're done with writing, you need to reset the parcel for reading:
        parcel.setDataPosition(0);

        // Reconstruct object from parcel and asserts:
        final QuestionFragment.ClassificationInProgress createdFromParcel = QuestionFragment.ClassificationInProgress.CREATOR.createFromParcel(parcel);
        assertFalse("Parcel is the same.", classificationInProgress == createdFromParcel);
        assertEquals(classificationInProgress, createdFromParcel);
    }
}
