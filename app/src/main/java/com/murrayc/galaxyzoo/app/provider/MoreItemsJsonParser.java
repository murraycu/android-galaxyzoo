package com.murrayc.galaxyzoo.app.provider;

import android.util.JsonReader;

import com.murrayc.galaxyzoo.app.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 10/8/14.
 */
public class MoreItemsJsonParser {
    public static List<Subject> parseMoreItemsResponseContent(final InputStream content) {
        final List<Subject> result = new ArrayList<>();

        final JsonReader reader;
        try {
            reader = new JsonReader(new InputStreamReader(content, "UTF-8"));
            reader.beginArray();
            while (reader.hasNext()) {
                while (reader.hasNext()) {
                    final Subject subject = parseMoreItemsJsonObjectSubject(reader);
                    if (subject != null) {
                        result.add(subject);
                    }
                }
            }
            reader.endArray();
            reader.close();
        } catch (final UnsupportedEncodingException e) {
            Log.info("parseMoreItemsResponseContent: UnsupportedEncodingException parsing JSON", e);
        } catch (final IOException e) {
            Log.info("parseMoreItemsResponseContent: IOException parsing JSON", e);
        } catch (final IllegalStateException e) {
            Log.info("parseMoreItemsResponseContent: IllegalStateException parsing JSON", e);
        }

        if (result.size() == 0) {
            Log.error("Failed. No JSON entities parsed."); //TODO: Use some constant error code?
        }

        //TODO: If this is 0 then something went wrong. Let the user know,
        //maybe via the parseMoreItemsJsonObjectSubject() return string..
        //For instance, the Galaxy-Zoo server could be down for maintenance (this has happened before),
        //or there could be some other network problem.
        return result;
    }

    private static Subject parseMoreItemsJsonObjectSubject(final JsonReader reader) throws IOException {
        reader.beginObject();

        final Subject result = new Subject();

        while (reader.hasNext()) {
            final String name = reader.nextName();
            switch (name) {
                case "id":
                    result.mId = reader.nextString();
                    break;
                case "zooniverse_id":
                    result.mZooniverseId = reader.nextString();
                    break;
                case "location":
                    parseMoreItemsJsonObjectSubjectLocation(reader, result);
                    break;
                default:
                    reader.skipValue();
            }
        }

        reader.endObject();
        return result;
    }

    private static void parseMoreItemsJsonObjectSubjectLocation(final JsonReader reader, final Subject result) throws IOException {
        reader.beginObject();

        while (reader.hasNext()) {
            final String name = reader.nextName();
            switch (name) {
                case "standard":
                    result.mLocationStandard = reader.nextString();
                    break;
                case "thumbnail":
                    result.mLocationThumbnail= reader.nextString();
                    break;
                case "inverted":
                    result.mLocationInverted = reader.nextString();
                    break;
                default:
                    reader.skipValue();
            }
        }

        reader.endObject();
    }

    public static class Subject {
        public String mId;
        public String mZooniverseId;
        public String mLocationStandard;
        public String mLocationThumbnail;
        public String mLocationInverted;
    }
}
