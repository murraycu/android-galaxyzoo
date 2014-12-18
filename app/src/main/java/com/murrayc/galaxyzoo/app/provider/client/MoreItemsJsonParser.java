/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.provider.client;

import android.util.JsonReader;

import com.murrayc.galaxyzoo.app.Log;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 10/8/14.
 */
public class MoreItemsJsonParser {
    public static List<ZooniverseClient.Subject> parseMoreItemsResponseContent(final String content) {
        final Reader reader = new StringReader(content);
        final List<ZooniverseClient.Subject> result = parseMoreItemsResponseContent(reader);

        try {
            reader.close();
        } catch (final IOException e) {
            Log.error("MoreItemsJsonParser.parseMoreItemsResponseContent(): StringReader.close() failed", e);
        }

        return result;
    }

    public static List<ZooniverseClient.Subject> parseMoreItemsResponseContent(final Reader contentReader) {
        final List<ZooniverseClient.Subject> result = new ArrayList<>();

        final JsonReader reader;
        try {
            reader = new JsonReader(contentReader);
            reader.beginArray();
            while (reader.hasNext()) {
                while (reader.hasNext()) {
                    final ZooniverseClient.Subject subject = parseMoreItemsJsonObjectSubject(reader);
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

    private static ZooniverseClient.Subject parseMoreItemsJsonObjectSubject(final JsonReader reader) throws IOException {
        reader.beginObject();

        String subjectId = null;
        String zooniverseId = null;
        Locations locations = null;

        while (reader.hasNext()) {
            final String name = reader.nextName();
            switch (name) {
                case "id":
                    subjectId = reader.nextString();
                    break;
                case "zooniverse_id":
                    zooniverseId = reader.nextString();
                    break;
                case "location":
                    locations = parseMoreItemsJsonObjectSubjectLocation(reader);
                    break;
                default:
                    reader.skipValue();
            }
        }

        reader.endObject();

        if (locations == null) {
            Log.error("parseMoreItemsJsonObjectSubject(): locations is null.");
            return null;
        }

        return new ZooniverseClient.Subject(subjectId, zooniverseId,
                locations.getLocationStandard(), locations.getLocationThumbnail(), locations.getLocationInverted());
    }

    /**
     * This is meant to be immutable.
     */
    final static class Locations {
        private final String mLocationStandard;
        private final String mLocationThumbnail;
        private final String mLocationInverted;

        Locations(final String locationStandard, final String locationThumbnail, final String locationInverted) {
            this.mLocationStandard = locationStandard;
            this.mLocationThumbnail = locationThumbnail;
            this.mLocationInverted = locationInverted;
        }

        public String getLocationStandard() {
            return mLocationStandard;
        }

        public String getLocationThumbnail() {
            return mLocationThumbnail;
        }

        public String getLocationInverted() {
            return mLocationInverted;
        }
    }

    private static Locations parseMoreItemsJsonObjectSubjectLocation(final JsonReader reader) throws IOException {
        reader.beginObject();

        String locationStandard = null;
        String locationThumbnail = null;
        String locationInverted = null;

        while (reader.hasNext()) {
            final String name = reader.nextName();
            switch (name) {
                case "standard":
                    locationStandard = reader.nextString();
                    break;
                case "thumbnail":
                    locationThumbnail = reader.nextString();
                    break;
                case "inverted":
                    locationInverted = reader.nextString();
                    break;
                default:
                    reader.skipValue();
            }
        }

        reader.endObject();

        return new Locations(locationStandard, locationThumbnail, locationInverted);
    }

}
