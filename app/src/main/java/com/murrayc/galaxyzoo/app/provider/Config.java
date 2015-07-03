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

    //See Config.coffee:production:
    //https://github.com/zooniverse/Galaxy-Zoo/blob/master/app/lib/config.coffee
    public static final Map<String, SubjectGroup> SUBJECT_GROUPS;

    public static final String SUBJECT_GROUP_ID_SLOAN = "50251c3b516bcb6ecb000002";
    public static final String SUBJECT_GROUP_ID_SLOAN_SINGLEBAND = "5514521e2f0eef2012000001";
    public static final String SUBJECT_GROUP_ID_GOODS_FULL = "551453e12f0eef21f2000001";



    static {
        SUBJECT_GROUPS = new HashMap<>();

        SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN /* SSID / sloan */,
                new SubjectGroup("sloan_tree.xml", true,
                  new DecisionTree.DiscussQuestion("sloan-11", "a-0", "a-1")));

        //We don't request items for all these groups any more, but we still want to load the
        //trees so can ask questions about items that have already been downloaded and stored in
        //the cache.
        //At some point we can remove some when we are sure they are unnecessary.
        SUBJECT_GROUPS.put("551456e02f0eef2535000001" /* candels_2epoch */,
                new SubjectGroup("candels_tree.xml", false,
                  new DecisionTree.DiscussQuestion("candels-17", "a-0", "a-1")));
        SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_GOODS_FULL /* goods_full */,
                new SubjectGroup("goods_full_tree.xml", false,
                  new DecisionTree.DiscussQuestion("goods_full-16", "a-0", "a-1")));
        SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN_SINGLEBAND /* sloan_singleband */,
                new SubjectGroup("sloan_singleband_tree.xml", false,
                        new DecisionTree.DiscussQuestion("sloan_singleband-11", "a-0", "a-1")));
    }
}
