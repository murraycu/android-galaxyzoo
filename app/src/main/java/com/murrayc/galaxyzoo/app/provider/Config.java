package com.murrayc.galaxyzoo.app.provider;

/**
 * The various URIs used to communicate with the server.
 */
public class Config {

    static private final String SERVER = "https://api.zooniverse.org/projects/galaxy_zoo/";

    /** REST uri for querying items.
     * Like, the Galaxy-Zoo website's code, this hard-codes the Group ID for the Sloan survey: */
    static final String QUERY_URI =
            SERVER + "groups/50251c3b516bcb6ecb000002/subjects?limit="; //Should have a number, such as 5, appended.


    static final String POST_URI =
            SERVER + "workflows/50251c3b516bcb6ecb000002/classifications";

    static final String LOGIN_URI =
            //"http://www.murrayc.com/galaxyzootestlogin"; //Avoid bothering the zooniverse server until we are more sure that this works.
            SERVER + "login";

    //TODO: This is used in the app, not in the content provider.
    //Maybe this whole class should be in the app, not the content provider.
    public static final String TALK_URI = "http://talk.galaxyzoo.org/#/subjects/";

    //TODO: This is used in the app, not in the content provider.
    //Maybe this whole class should be in the app, not the content provider.
    //TODO: Is there a stable URI for this?
    public static final String ICONS_CSS_URI = "http://www.galaxyzoo.org/application-2014-08-18_21-36-52.css";

    static private final String STATIC_IMAGES_SERVER = "http://static.zooniverse.org/www.galaxyzoo.org/images/";

    //This is one big image file containing many icons,
    //for use with the CSS Sprites technique.
    public static final String ICONS_URI = STATIC_IMAGES_SERVER + "workflow.png";

    //This is one big image file containing many icons,
    //for use with the CSS Sprites technique.
    //Note: Do not use the examples.png which is also there - it seems to be outdated and unused.
    public static final String EXAMPLES_URI = STATIC_IMAGES_SERVER + "examples.jpg";

    //Add the example ID and .jpg to this:
    public static final String FULL_EXAMPLE_URI = STATIC_IMAGES_SERVER + "examples/";
}
