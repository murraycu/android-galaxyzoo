package com.murrayc.galaxyzoo.app.provider.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

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

    static List<String> listOfStringsFromJsonArray(final JsonObject jsonObject, final String fieldName) {
        final List<String> result = new ArrayList<>();
        final JsonArray jsonArray = jsonObject.getAsJsonArray(fieldName);
        for (final JsonElement jsonElement : jsonArray) {
            if (jsonElement == null) {
                continue;
            }

            final String text = jsonElement.getAsString();
            if (text != null && !text.isEmpty()) {
                result.add(text);
            }
        }

        return result;
    }
}
