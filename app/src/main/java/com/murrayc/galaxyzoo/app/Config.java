/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

/**
 * See also com.murrayc.galaxyzoo.app.provider.Config;
 *
 * Created by murrayc on 10/10/14.
 */
final class Config {
    //Maybe this whole class should be in the app, not the content provider.
    public static final String TALK_URI = "http://talk.galaxyzoo.org/#/subjects/";

    //Maybe this whole class should be in the app, not the content provider.
    public static final String EXAMINE_URI = "http://www.galaxyzoo.org/#/examine/";

    //Maybe this whole class should be in the app, not the content provider.
    //TODO: Is there a stable URI for this?
    //public static final String ICONS_CSS_URI = "http://www.galaxyzoo.org/application-2014-09-05_13-06-46.css";

    //We hard-code this.
    //Alternatively, we could hard-code the removal of this question from the XML
    //when generating the XML file,
    //and then always ask the question at the end via Java code.
    public static final CharSequence QUESTION_ID_DISCUSS = "sloan-11";
    public static final CharSequence ANSWER_ID_DISCUSS_YES = "a-0";
    public static final String ANSWER_ID_DISCUSS_NO = "a-1";

    static private final String STATIC_IMAGES_SERVER = "http://static.zooniverse.org/www.galaxyzoo.org/images/";

    //This is one big image file containing many icons,
    //for use with the CSS Sprites technique.
    //public static final String ICONS_URI = STATIC_IMAGES_SERVER + "workflow.png";

    //This is one big image file containing many icons,
    //for use with the CSS Sprites technique.
    //Note: Do not use the examples.png which is also there - it seems to be outdated and unused.
    //public static final String EXAMPLES_URI = STATIC_IMAGES_SERVER + "examples.jpg";

    //Add the example ID and .jpg to this:
    public static final String FULL_EXAMPLE_URI = STATIC_IMAGES_SERVER + "examples/";

    // galaxyzoo.org also has a register page, but it's only visible as part of the login page
    // (not an actual page either) after clicking a button.
    // Hopefully there is no disadvantage to using the general zooniverse pages instead.
    public static final String REGISTER_URI = "https://www.zooniverse.org/signup";
    public static final String FORGET_PASSWORD_URI = "https://www.zooniverse.org/password/reset";
}
