package com.murrayc.galaxyzoo.app;

/**
 * See also com.murrayc.galaxyzoo.app.provider.Config;
 *
 * Created by murrayc on 10/10/14.
 */
public class Config {
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
    //These are actually specified in strings.xml instead, because that's the only
    //easy way to specify (clickable) HTML for a TextView.
    //public static final String REGISTER_URI = "https://www.zooniverse.org/signup";
    //public static final String FORGET_PASSWORD_URI = "https://www.zooniverse.org/password/reset";
}
