package com.murrayc.galaxyzoo.app.provider.rest;

import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;


/**
 * Created by murrayc on 7/2/14.
 */
public class GalaxyZooPostResponseHandler implements ResponseHandler<String> {

    private final ItemsContentProvider mContentProvider;

    public GalaxyZooPostResponseHandler(ItemsContentProvider contentProvider) {
        this.mContentProvider = contentProvider;
    }

    /*
    * Handles the response from the RESTful server.
    */
    @Override
    public String handleResponse(final HttpResponse response) {
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            return "Did not receive the 201 Created status code: " + response.getStatusLine().toString();
        }

        //The JSON response seems to be just {},
        //so we don't need to parse it.

        return null; //Means success by our convention.
    }
}
