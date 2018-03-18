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

import com.murrayc.galaxyzoo.app.DecisionTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The various URIs used to communicate with the server.
 * The child URIs are built in ZooniverseClient.
 *
 * See also com.murrayc.galaxyzoo.app.Config.
 */
public final class Config {

    //The real server:
    public static final String SERVER = "https://www.zooniverse.org/api/";
    //For testing:
    //public static final String SERVER = "https://dev.zooniverse.org/projects/galaxy_zoo/";

    //This is an attempt to reduce the amount of Network and Disk IO
    //that the system does, because even when using a Thread (with Thread.MIN_PRIORITY) instead of
    //AsyncTask, the UI is non responsive during this work.
    //For instance, buttons appear to be pressed, but their clicked listeners are not called.
    //(However, this problem was avoided by using a SyncAdapter: http://www.murrayc.com/permalink/2015/01/22/android-galaxyzoo-network-io-and-ui-responsiveness/ )
    //It also allows us to get a mix of items from different groups.
    public static final int MAXIMUM_DOWNLOAD_ITEMS = 5;

    public static List<String> getSubjectGroupsToUseForNewQueries() {
        return SUBJECT_GROUPS_TO_USE_FOR_NEW_QUERIES;
    }

    public static class SubjectGroup {
        final String filename;
        final boolean useForNewQueries;
        final DecisionTree.DiscussQuestion discussQuestion;

        public SubjectGroup(final String filename, final boolean useForNewQueries, final DecisionTree.DiscussQuestion discussQuestion) {
            this.filename = filename;
            this.useForNewQueries = useForNewQueries;

            //We hard-code this.
            //Alternatively, we could hard-code the removal of this question from the XML
            //when generating the XML file,
            //and then always ask the question at the end via Java code.
            this.discussQuestion = discussQuestion;
        }

        public String getFilename() {
            return filename;
        }

        public boolean getUseForNewQueries() {
            return useForNewQueries;
        }

        public DecisionTree.DiscussQuestion getDiscussQuestion() {
            return discussQuestion;
        }
    }

    public static final Map<String, SubjectGroup> SUBJECT_GROUPS;
    private static final List<String> SUBJECT_GROUPS_TO_USE_FOR_NEW_QUERIES;

    //See Config.coffee:production:
    //https://github.com/zooniverse/Galaxy-Zoo/blob/master/app/lib/config.coffee
    public static final String SUBJECT_GROUP_ID_GAMA_15 = "5853fab395ad361930000003";

    static {
        SUBJECT_GROUPS = new HashMap<>();

        //Production:
        {
            // TODO: Avoid re-parsing the whole XML tree when surveys share a tree.
            SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_GAMA_15, //GAMA 15
                    new SubjectGroup("gama_tree.xml", true,
                            new DecisionTree.DiscussQuestion("gama-11", "a-0", "a-1")));

        }

        SUBJECT_GROUPS_TO_USE_FOR_NEW_QUERIES = new ArrayList<>();
        for (final Map.Entry<String, SubjectGroup> entry : SUBJECT_GROUPS.entrySet()) {
            final Config.SubjectGroup group = entry.getValue();
            if (group.getUseForNewQueries()) {
                SUBJECT_GROUPS_TO_USE_FOR_NEW_QUERIES.add(entry.getKey());
            }
        }
    }
}
