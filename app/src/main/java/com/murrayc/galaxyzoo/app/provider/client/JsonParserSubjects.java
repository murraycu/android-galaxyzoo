package com.murrayc.galaxyzoo.app.provider.client;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 3/27/18.
 */

public class JsonParserSubjects {

    /** A custom GSON deserializer,
     * so we can create Subject objects using the constructor.
     * We want to do so Subject can remain an immutable class.
     */
    static class SubjectsResponseDeserializer implements JsonDeserializer<ZooniverseClient.SubjectsResponse> {
        public ZooniverseClient.SubjectsResponse deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            final JsonArray jsonSubjects = jsonObject.getAsJsonArray("subjects");

            // Parse each subject:
            final List<ZooniverseClient.Subject> subjects = new ArrayList<>();
            for (final JsonElement jsonSubject : jsonSubjects) {
                final JsonObject asObject = jsonSubject.getAsJsonObject();
                final ZooniverseClient.Subject subject = deserializeSubjectFromJsonObject(asObject);
                subjects.add(subject);
            }

            // final JsonElement jsonMetadata = jsonObject.get("metadata");

            final ZooniverseClient.SubjectsResponse result = new ZooniverseClient.SubjectsResponse(subjects);
            return result;
        }

        @Nullable
        private ZooniverseClient.Subject deserializeSubjectFromJsonObject(JsonObject jsonObject) {
            final String id = JsonUtils.getString(jsonObject, "id");
            final String zooniverseId = JsonUtils.getString(jsonObject, "zooniverse_id");
            final String groupId = JsonUtils.getString(jsonObject, "group_id");

            String locationStandard = null; //TODO: Others too.
            List<String> locations = null;
            final JsonElement jsonElementLocations = jsonObject.get("locations");
            if (jsonElementLocations != null) {
                locations = deserializeLocationsFromJsonElement(jsonElementLocations);
                if (locations != null && !locations.isEmpty()) {
                    locationStandard = locations.get(0);
                }
            }

            return new ZooniverseClient.Subject(id, zooniverseId, groupId, locationStandard, null, null);
        }

        private List<String> deserializeLocationsFromJsonElement(final JsonElement jsonElement) {
            final JsonArray jsonLocations = jsonElement.getAsJsonArray();
            if (jsonLocations == null) {
                return null;
            }

            // Parse each location:
            final List<String> locations = new ArrayList<>();
            for (final JsonElement jsonLocation : jsonLocations) {
                final JsonObject asObject = jsonLocation.getAsJsonObject();
                final String url = JsonUtils.getString(asObject, "image/jpeg");
                if (url != null) {
                    locations.add(url);
                }
            }

            return locations;
        }
    }

    /** A custom GSON deserializer,
     * so we can create Subject objects using the constructor.
     * We want to do so Subject can remain an immutable class.
     */
    static class SubjectDeserializer implements JsonDeserializer<ZooniverseClient.Subject> {
        public ZooniverseClient.Subject deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            return deserializeSubjectFromJsonObject(jsonObject);
        }

        @Nullable
        private ZooniverseClient.Subject deserializeSubjectFromJsonObject(JsonObject jsonObject) {
            final String id = JsonUtils.getString(jsonObject, "id");
            final String zooniverseId = JsonUtils.getString(jsonObject, "zooniverse_id");
            final String groupId = JsonUtils.getString(jsonObject, "group_id");

            final JsonElement jsonElementLocations = jsonObject.get("locations");
            if (jsonElementLocations == null) {
                return null;
            }

            final JsonObject jsonObjectLocations = jsonElementLocations.getAsJsonObject();
            if (jsonObjectLocations == null) {
                return null;
            }

            final String locationStandard = JsonUtils.getString(jsonObjectLocations, "image/jpg");

            return new ZooniverseClient.Subject(id, zooniverseId, groupId, locationStandard, null, null);
        }
    }
}
