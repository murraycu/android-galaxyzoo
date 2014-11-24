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

import com.murrayc.galaxyzoo.app.LoginActivity;

/**
 * Created by murrayc on 5/26/14.
 */
public class LoginActivityTest
        extends ActivityUnitTestCase<LoginActivity> {

    private LoginActivity mActivity;

    public LoginActivityTest() {
        super(LoginActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        TestUtils.setTheme(this);

        startActivity(new Intent(getInstrumentation().getTargetContext(), LoginActivity.class), null, null);

        mActivity = getActivity();
        assertNotNull(mActivity);
    }

    public void testExists() {
        assertNotNull(mActivity);
    }

    public void testStateDestroy() {
        mActivity.finish();
        mActivity = this.getActivity();
        assertNotNull(mActivity);
    }
}
