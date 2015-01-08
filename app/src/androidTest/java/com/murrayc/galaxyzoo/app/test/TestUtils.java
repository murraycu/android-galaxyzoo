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

import android.test.ActivityUnitTestCase;
import android.view.ContextThemeWrapper;

import com.murrayc.galaxyzoo.app.R;

/**
 * Created by murrayc on 11/7/14.
 */
final class TestUtils {
    static void setTheme(final ActivityUnitTestCase<?> testCase) {
        //Avoid this exception:
        //java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
        final ContextThemeWrapper context = new ContextThemeWrapper(testCase.getInstrumentation().getTargetContext(), R.style.AppTheme);
        testCase.setActivityContext(context);
    }
}
