package com.murrayc.galaxyzoo.app.provider;

/**
 * The various URIs used to communicate with the server.
 */
public class Config {

    static private final String SERVER = "https://api.zooniverse.org/projects/galaxy_zoo/";
    /** REST uri for querying items.
     * Like, the Galaxy-Zoo website's code, this hard-codes the Group ID for the Sloan survey: */
    static final String QUERY_URI =
            SERVER + "groups/50251c3b516bcb6ecb000002/subjects?limit=5";


    static final String POST_URI =
            SERVER + "workflows/50251c3b516bcb6ecb000002/classifications";

    static final String LOGIN_URI =
            //"http://www.murrayc.com/galaxyzootestlogin"; //Avoid bothering the zooniverse server until we are more sure that this works.
            SERVER + "login";

    //TODO: This is used in the app, not in the content provider.
    //Maybe this whole class should be in the app, not the content provider.
    public static final String TALK_URI = "http://talk.galaxyzoo.org/#/subjects/";
}
