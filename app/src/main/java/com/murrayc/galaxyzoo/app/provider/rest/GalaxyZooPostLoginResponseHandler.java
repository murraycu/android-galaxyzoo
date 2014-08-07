package com.murrayc.galaxyzoo.app.provider.rest;

import android.content.Context;
import android.content.SharedPreferences;
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
public class GalaxyZooPostLoginResponseHandler implements ResponseHandler<GalaxyZooPostLoginResponseHandler.LoginResult> {

    private final ItemsContentProvider mContentProvider;

    public GalaxyZooPostLoginResponseHandler(ItemsContentProvider contentProvider) {
        this.mContentProvider = contentProvider;
    }

    /*
    * Handles the response from the RESTful server.
    */
    @Override
    public LoginResult handleResponse(final HttpResponse response) {
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            Log.error("Did not receive the 200 OK status code: " + response.getStatusLine().toString());
        }

        final String responseString;
        try {
            responseString = new BasicResponseHandler().handleResponse(response);
            Log.info("Login response string", responseString);
            return parseJson(responseString);
        } catch (final IOException e) {
            Log.error("Exception from BasicResponseHandler:", e);
        }

        return null; //Means success by our convention.
    }

    private LoginResult parseJson(final String responseString) {
        //A failure by default.
        LoginResult result = new LoginResult(false, null, null);

        JSONTokener tokener = new JSONTokener(responseString);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(tokener);
        } catch (JSONException e) {
            Log.error("JSON parsing failed.", e);
            return result;
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

                return new LoginResult(true, name, apiKey);

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
                return result;
            }
        } catch (final JSONException e) {
            e.printStackTrace();
            return result;
        }
    }

    public static class LoginResult {
        private final boolean success;
        private final String name;
        private final String apiKey;

        public LoginResult(boolean success, final String name, final String apiKey) {
            this.success = success;
            this.name = name;
            this.apiKey = apiKey;
        }

        public String getApiKey() {
            return apiKey;
        }

        public boolean getSuccess() {
            return success;
        }

        public String getName() {
            return name;
        }
    }
}
