package com.murrayc.galaxyzoo.app.provider;

/**
 * The various URIs used to communicate with the server.
 * See also com.murrayc.galaxyzoo.app.Config.
 */
public class Config {

    static private final String SERVER = "https://api.zooniverse.org/projects/galaxy_zoo/";

    /**
     * REST uri for querying items.
     * Like, the Galaxy-Zoo website's code, this hard-codes the Group ID for the Sloan survey:
     */
    static final String QUERY_URI =
            SERVER + "groups/50251c3b516bcb6ecb000002/subjects?limit="; //Should have a number, such as 5, appended.
    static final String POST_URI =
            SERVER + "workflows/50251c3b516bcb6ecb000002/classifications";
    static final String LOGIN_URI =
            //"http://www.murrayc.com/galaxyzootestlogin"; //Avoid bothering the zooniverse server until we are more sure that this works.
            SERVER + "login";
}
