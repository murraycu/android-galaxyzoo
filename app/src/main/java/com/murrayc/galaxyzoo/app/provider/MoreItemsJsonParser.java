package com.murrayc.galaxyzoo.app.provider;

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
 * Created by murrayc on 10/8/14.
 */
public class MoreItemsJsonParser {
    public static List<Subject> parseMoreItemsResponseContent(final InputStream content) {
        final String str;
        try {
            str = HttpUtils.getStringFromInputStream(content);
        } catch (IOException e) {
            Log.error("parseMoreItemsResponseContent(): Exception while getting string from input stream", e);
            return null;
        }

        final List<Subject> result = new ArrayList<>();

        final JSONTokener tokener = new JSONTokener(str);
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(tokener);
        } catch (JSONException e) {
            Log.error("JSON parsing failed.", e);
            return result;
        }

        for (int i = 0; i < jsonArray.length(); ++i) {
            JSONObject obj;
            try {
                obj = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                Log.error("JSON parsing of object failed.", e);
                return result;
            }

            final Subject subject = parseMoreItemsJsonObjectSubject(obj);
            if (subject != null) {
                result.add(subject);
            }
        }

        //TODO: If this is 0 then something went wrong. Let the user know,
        // only flush old state now that new state has arrived
        if (result.size() == 0) {
            Log.error("Failed. No JSON entities parsed."); //TODO: Use some constant error code?
        }

        //maybe via the parseMoreItemsJsonObjectSubject() return string..
        //For instance, the Galaxy-Zoo server could be down for maintenance (this has happened before),
        //or there could be some other network problem.
        return result;
    }

    private static Subject parseMoreItemsJsonObjectSubject(final JSONObject objSubject) {
        try {
            final Subject subject = new Subject();
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

    public static class Subject {
        public String mId;
        public String mZooniverseId;
        public String mLocationStandard;
        public String mLocationThumbnail;
        public String mLocationInverted;
    }
}
