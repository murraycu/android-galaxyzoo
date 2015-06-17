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

package com.murrayc.galaxyzoo.app.ui.test;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.Button;

import com.murrayc.galaxyzoo.app.ClassifyActivity;
import com.murrayc.galaxyzoo.app.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Created by murrayc on 5/26/14.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ClassifyActivityUiTest {

    @Rule
    public ActivityTestRule<ClassifyActivity> mActivityRule = new ActivityTestRule<>(
            ClassifyActivity.class);


    @Test
    public void testFirstQuestion() {
        onView(withId(R.id.textViewTitle)).check(matches(withText("Shape")));
        onView(withId(R.id.textViewText)).check(matches(withText("Is the galaxy simply smooth and rounded, with no sign of a disk?")));
        onView(allOf(instanceOf(Button.class), withText("Smooth"))).check(matches(isDisplayed()));
    }


    @Test
    public void testNextQuestion() {
        onView(allOf(instanceOf(Button.class), withText("Smooth"))).perform(click());

        onView(withId(R.id.textViewTitle)).check(matches(withText("Round")));
        onView(withId(R.id.textViewText)).check(matches(withText("How rounded is it?")));
    }

    //TODO: How can we click on the option menu item:
    /*
    @Test
    public void testAbout() {
        onView(withId(R.id.option_menu_item_about)).perform(click());
        onView(withId(R.id.textViewAbout)).check(matches(isDisplayed()));
    }
    */
}
