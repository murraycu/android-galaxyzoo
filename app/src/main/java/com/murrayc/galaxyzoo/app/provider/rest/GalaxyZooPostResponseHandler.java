package com.murrayc.galaxyzoo.app.provider.rest;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


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
