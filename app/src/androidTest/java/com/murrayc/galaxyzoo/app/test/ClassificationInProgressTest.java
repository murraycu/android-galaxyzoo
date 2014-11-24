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
