package com.murrayc.galaxyzoo.app.provider.rest;

import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;


/**
 * Created by murrayc on 7/2/14.
 */
public class GalaxyZooPostLoginResponseHandler implements ResponseHandler<String> {

    private final ItemsContentProvider mContentProvider;

    public GalaxyZooPostLoginResponseHandler(ItemsContentProvider contentProvider) {
        this.mContentProvider = contentProvider;
    }

    /*
    * Handles the response from the RESTful server.
    */
    @Override
    public String handleResponse(final HttpResponse response) {
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            return "Did not receive the 200 OK status code: " + response.getStatusLine().toString();
        }

        final String responseString;
        try {
            responseString = new BasicResponseHandler().handleResponse(response);
            Log.info("Login response string", responseString);
            parseJson(responseString);
        } catch (final IOException e) {
            Log.error("Exception from BasicResponseHandler:", e);
        }


        //TODO: Parse the response to get the api_key.

        return null; //Means success by our convention.
    }

    private void parseJson(final String responseString) {
        JSONTokener tokener = new JSONTokener(responseString);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(tokener);
        } catch (JSONException e) {
            Log.error("JSON parsing failed.", e);
            return;
        }

        try {
            if(TextUtils.equals(jsonObject.getString("success"), "true")) {
                Log.info("Login succeeded.");

                //TODO: Store the name and api_key for later use when uploading classifications.
                //final String id = jsonObject.getString("id");
                final String apiKey = jsonObject.getString("api_key");
                //final String avatar = jsonObject.getString("avatar");
                //final long classificationCount = jsonObject.getLong("classification_count");
                //final String email = jsonObject.getString("email");
                //final long favoriteCount = jsonObject.getLong("favorite_count");
                final String name = jsonObject.getString("name");
                //final String zooniverseId = jsonObject.getString("zooniverse_id");

                //Then there is an object called "project", like so:
                /*
                "project":{
                    "classification_count":66,
                            "favorite_count":2,
                            "groups":{
                        "50251c3b516bcb6ecb000002":{
                            "classification_count":66,
                                    "name":"sloan"
                        }
                    },
                    "splits":{
                    }
                }
                */
            } else {
                Log.info("Login failed.");

                final String message = jsonObject.getString("message");
                Log.info("Login failure message", message);
            }
        } catch (final JSONException e) {
            e.printStackTrace();
        }

    }
}
