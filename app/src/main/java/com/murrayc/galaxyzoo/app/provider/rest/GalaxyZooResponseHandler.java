package com.murrayc.galaxyzoo.app.provider.rest;

import com.murrayc.galaxyzoo.app.provider.HttpUtils;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;
import com.murrayc.galaxyzoo.app.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by murrayc on 7/2/14.
 */
public class GalaxyZooResponseHandler {

    public static List<ItemsContentProvider.Subject> parseContent(final InputStream content) throws IOException {
        final String str = HttpUtils.getStringFromInputStream(content);

        final List<ItemsContentProvider.Subject> result = new ArrayList<>();

        JSONTokener tokener = new JSONTokener(str);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(tokener);
        } catch (JSONException e) {
            Log.error("JSON parsing failed.", e);
            return result;
        }

        for(int i = 0; i < jsonArray.length(); ++i) {
            JSONObject obj = null;
            try {
                obj = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                Log.error("JSON parsing of object failed.", e);
                return result;
            }

            final ItemsContentProvider.Subject subject = parseJsonObjectSubject(obj);
            if (subject != null) {
                result.add(subject);
            }
        }

        //TODO: If this is 0 then something went wrong. Let the user know,
        // only flush old state now that new state has arrived
        if (result.size() == 0) {
            Log.error("Failed. No JSON entities parsed."); //TODO: Use some constant error code?
        }

        //maybe via the handleResponse() return string, which seems to be for whatever we want.
        //For instance, the Galaxy-Zoo server could be down for maintenance (this has happened before),
        //or there could be some other network problem.
        return result;
    }

    private static ItemsContentProvider.Subject parseJsonObjectSubject(final JSONObject objSubject) {
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

            return subject;
        } catch (JSONException e) {
            Log.error("JSON parsing of object fields failed.", e);
        }

        return null;
    }
}
