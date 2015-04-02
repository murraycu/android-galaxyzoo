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

package com.murrayc.galaxyzoo.app.provider;

import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;

/**
 * The various URIs used to communicate with the server.
 * The child URIs are built in ZooniverseClient.
 *
 * See also com.murrayc.galaxyzoo.app.Config.
 */
public final class Config {

    public static final String SERVER = "https://api.zooniverse.org/projects/galaxy_zoo/";

    public static class SubjectGroup {
        final String filename;
        final boolean useForNewQueries;

        public SubjectGroup(final String filename, boolean useForNewQueries) {
            this.filename = filename;
            this.useForNewQueries = useForNewQueries;
        }

        public String getFilename() {
            return filename;
        }

        public boolean getUseForNewQueries() {
            return useForNewQueries;
        }
    };

    //See Config.coffee:production:
    //https://github.com/zooniverse/Galaxy-Zoo/blob/master/app/lib/config.coffee
    public static final Map<String, SubjectGroup> SUBJECT_GROUPS;

    public static final String SUBJECT_GROUP_ID_SLOAN = "50251c3b516bcb6ecb000002";

    static {
        SUBJECT_GROUPS = new HashMap<>();

        //We don't request items for this group any more, but we still want to load the
        //tree so can ask questions about items that have already been downloaded and stored in
        //the cache.
        //At some point we can remove this when we are sure it is unnecessary.
        SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN /* SSID / sloan */,
                new SubjectGroup("sloan_tree.xml", false));

        SUBJECT_GROUPS.put("551456e02f0eef2535000001" /* candels_2epoch */,
                new SubjectGroup("candels_tree.xml", true));
        SUBJECT_GROUPS.put("551453e12f0eef21f2000001" /* goods_full */,
                new SubjectGroup("goods_full_tree.xml", true));
    }
}
