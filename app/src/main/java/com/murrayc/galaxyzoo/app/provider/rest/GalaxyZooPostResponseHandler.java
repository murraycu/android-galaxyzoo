package com.murrayc.galaxyzoo.app.provider.rest;

import com.murrayc.galaxyzoo.app.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;


/**
 * Created by murrayc on 7/2/14.
 */
public class GalaxyZooPostResponseHandler implements ResponseHandler<Boolean> {

    public GalaxyZooPostResponseHandler() {
    }

    /*
    * Handles the response from the RESTful server.
    */
    @Override
    public Boolean handleResponse(final HttpResponse response) {
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            Log.error("Did not receive the 201 Created status code: " + response.getStatusLine().toString());
            return false;
        }

        //The JSON response seems to be just {},
        //so we don't need to parse it.

        return true; //Means success.
    }
}
