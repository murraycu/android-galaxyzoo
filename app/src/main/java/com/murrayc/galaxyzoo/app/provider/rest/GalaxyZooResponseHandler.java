package com.murrayc.galaxyzoo.app.provider.rest;

import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;
import com.murrayc.galaxyzoo.app.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
public class GalaxyZooResponseHandler implements ResponseHandler<Boolean> {

    private final ItemsContentProvider mContentProvider;

    public GalaxyZooResponseHandler(ItemsContentProvider contentProvider) {
        this.mContentProvider = contentProvider;
    }

    /*
    * Handles the response from the RESTful server.
    */
    @Override
    public Boolean handleResponse(HttpResponse response) {
        try {
            final int newCount = parseEntity(response.getEntity());

            // only flush old state now that new state has arrived
            if (newCount <= 0) {
                Log.error("Failed. No JSON entities parsed."); //TODO: Use some constant error code?
                return false;
            }

        } catch (IOException e) {
            Log.error("Exception from parseEntity", e);
            return false;
        }

        return true;
    }

    private int parseEntity(HttpEntity entity) throws IOException {
        final InputStream content = entity.getContent();
        final InputStreamReader inputReader = new InputStreamReader(content);
        final BufferedReader reader = new BufferedReader(inputReader);


        int inserted = 0;

        final StringBuilder builder = new StringBuilder();
        for (String line = null; (line = reader.readLine()) != null;) {
            builder.append(line).append("\n");
        }

        JSONTokener tokener = new JSONTokener(builder.toString());
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(tokener);
        } catch (JSONException e) {
            Log.error("JSON parsing failed.", e);
            return inserted;
        }

        for(int i = 0; i < jsonArray.length(); ++i) {
            JSONObject obj = null;
            try {
                obj = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                Log.error("JSON parsing of object failed.", e);
                return inserted;
            }

            if(parseJsonObjectSubject(obj)) {
                inserted++;
            }
        }

        //TODO: If this is 0 then something went wrong. Let the user know,
        //maybe via the handleResponse() return string, which seems to be for whatever we want.
        //For instance, the Galaxy-Zoo server could be down for maintenance (this has happened before),
        //or there could be some other network problem.
        return inserted;
    }

    private boolean parseJsonObjectSubject(final JSONObject objSubject) {
        try {
            final ItemsContentProvider.Subject subject = new ItemsContentProvider.Subject();
            subject.mId = objSubject.getString("id");
            subject.mZooniverseId = objSubject.getString("zooniverse_id");
            final JSONObject objLocation = objSubject.getJSONObject("location");
            if (objLocation != null) {
                subject.mLocationStandard = objLocation.getString("standard");
                subject.mLocationThumbnail = objLocation.getString("thumbnail");
                subject.mLocationInverted = objLocation.getString("inverted");
            }

            insertIntoContentProvider(subject);
            return true;
        } catch (JSONException e) {
            Log.error("JSON parsing of object fields failed.", e);
        }

        return false;
    }

    private void insertIntoContentProvider(final ItemsContentProvider.Subject subject) {
        mContentProvider.addSubject(subject);
    }
}
