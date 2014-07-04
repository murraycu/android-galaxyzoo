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
public class GalaxyZooResponseHandler implements ResponseHandler<String> {

    private final ItemsContentProvider mContentProvider;

    public GalaxyZooResponseHandler(ItemsContentProvider contentProvider) {
        this.mContentProvider = contentProvider;
    }

    /*
    * Handles the response from the RESTful server.
    */
    @Override
    public String handleResponse(HttpResponse response) {
        try {
            int newCount = parseEntity(response.getEntity());

            // only flush old state now that new state has arrived
            if (newCount > 0) {
                //TODO? deleteOld();
            }

        } catch (IOException e) {
            // use the exception to avoid clearing old state, if we can not
            // get new state.  This way we leave the application with some
            // data to work with in absence of network connectivity.

            // we could retry the request for data in the hope that the network
            // might return.
        }

        return null; //TODO?
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

            parseJsonObjectClassification(obj);
        }

        return inserted;
    }

    private void parseJsonObjectClassification(final JSONObject objClassification) {
        try {
            //mId = obj.getString("id");
            //mCreatedAt = obj.getString("created_at");
            //mProjectId = obj.getString("project_id");
            //item.mSubjectIds = obj.getJSONArray("subject_ids");
            final JSONArray jsonArraySubjects = objClassification.getJSONArray("subjects");

            for(int i = 0; i < jsonArraySubjects.length(); ++i) {
                final JSONObject objSubject = jsonArraySubjects.getJSONObject(i);
                if (objSubject == null) {
                    continue;
                }

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
            }
        } catch (JSONException e) {
            Log.error("JSON parsing of object fields failed.", e);
        }
    }

    private void insertIntoContentProvider(final ItemsContentProvider.Subject subject) {
        mContentProvider.addSubject(subject);
    }
}
