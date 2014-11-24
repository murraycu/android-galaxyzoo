/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

import com.murrayc.galaxyzoo.app.ClassifyActivity;

/**
 * Created by murrayc on 5/26/14.
 */
public class ClassifyActivityTest
        extends ActivityUnitTestCase<ClassifyActivity> {

    private ClassifyActivity mActivity;

    public ClassifyActivityTest() {
        super(ClassifyActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        TestUtils.setTheme(this);

        startActivity(new Intent(getInstrumentation().getTargetContext(), ClassifyActivity.class), null, null);

        mActivity = getActivity();
        assertNotNull(mActivity);
    }

    public void testExists() {
        assertNotNull(mActivity);
    }

    public void testStateDestroy() {
        final String TEST_ITEM_ID = "test123456789";
        mActivity.setItemId(TEST_ITEM_ID);
        mActivity.finish();
        mActivity = this.getActivity();
        assertNotNull(mActivity);
        assertEquals(mActivity.getItemId(), itemId);
    }
}
