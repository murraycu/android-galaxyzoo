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

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.murrayc.galaxyzoo.app.ClassifyActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by murrayc on 5/26/14.
 */
@RunWith(AndroidJUnit4.class)
public class ClassifyActivityTest {
    // This must be public, or we'll get this exception:
    // org.junit.internal.runners.rules.ValidationError: The @Rule 'testRule' must be public
    // Note: If the third argument (launchActivity) is not false (really), the tests will fail because the
    // activity cannot be launched when you call launchActivity().
    @Rule
    public ActivityTestRule<ClassifyActivity> testRule = new ActivityTestRule<>(ClassifyActivity.class, false, false);

    private ClassifyActivity mActivity;

    @Before
    public void setUp() throws Exception {
        TestUtils.setTheme();

        final Intent intent = new Intent();
        mActivity = testRule.launchActivity(intent);
        assertNotNull(mActivity);
    }

    @Test
    public void testExists() {
        assertNotNull(mActivity);
    }

    @Test
    public void testStateDestroy() {
        final String TEST_ITEM_ID = "test123456789";
        mActivity.setItemId(TEST_ITEM_ID);
        mActivity.finish();

        // Based on this:
        // http://stackoverflow.com/a/33213279/1123654
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivity.recreate();
            }
        });

        assertEquals(TEST_ITEM_ID, mActivity.getItemId());

        //TODO: Do something like this too:
        //onView(withText("a string depending on XXX value")).check(doesNotExist())
    }
}
