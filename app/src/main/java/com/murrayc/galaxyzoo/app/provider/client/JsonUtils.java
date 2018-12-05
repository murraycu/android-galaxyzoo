package com.murrayc.galaxyzoo.app.provider.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by murrayc on 3/27/18.
 */

public class JsonUtils {
    static String getString(final JsonObject jsonObject, final String name) {
        final JsonElement jsonElementId = jsonObject.get(name);
        if (jsonElementId == null) {
            // The field does not exist in the JSON object.
            return null;
        }

        if (jsonElementId.isJsonNull()) {
            // The field is null in the JSON object.
            return null;
        }

        return jsonElementId.getAsString();
    }

    static boolean getBoolean(final JsonObject jsonObject, final String name) {
        final JsonElement jsonElementId = jsonObject.get(name);
        if (jsonElementId == null) {
            // The field does not exist in the JSON object.
            return false;
        }

        if (jsonElementId.isJsonNull()) {
            // The field is null in the JSON object.
            return false;
        }

        return jsonElementId.getAsBoolean();
    }
}
