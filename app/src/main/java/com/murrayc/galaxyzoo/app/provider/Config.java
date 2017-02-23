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
    public static final String SERVER = "https://www.galaxyzoo.org/_ouroboros_api/projects/galaxy_zoo/";
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
    public static final String SUBJECT_GROUP_ID_FERENGI2 = "58417dcb9afc3a007d000001";
    public static final String SUBJECT_GROUP_ID_DECALS_DR2 = "56f3d4645925d95984000001";
    public static final String SUBJECT_GROUP_ID_SDSS_LOST_SET = "56f2b5ed5925d9004200c775";
    public static final String SUBJECT_GROUP_ID_MISSING_MANGA = "5894999f7d25c7236f000001";
    public static final String SUBJECT_GROUP_ID_GAMA_09 = "5853fa7b95ad361930000001";
    //public static final String SUBJECT_GROUP_ID_DECALS = "55db7cf01766276e7b000001";
    //public static final String SUBJECT_GROUP_ID_ILLUSTRIS = "55db71251766276613000001";
    //public static final String SUBJECT_GROUP_ID_SLOAN = "50251c3b516bcb6ecb000002";
    //public static final String SUBJECT_GROUP_ID_SLOAN_SINGLEBAND = "5514521e2f0eef2012000001";
    //public static final String SUBJECT_GROUP_ID_GOODS_FULL = "551453e12f0eef21f2000001";

    static {
        SUBJECT_GROUPS = new HashMap<>();

        //Production:
        {
            SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_GAMA_09, //GAMA 09
                    new SubjectGroup("gama_tree.xml", true,
                            new DecisionTree.DiscussQuestion("gama-11", "a-0", "a-1")));
            SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_MISSING_MANGA, //Missing Manga
                    new SubjectGroup("sloan_tree.xml", true,
                            new DecisionTree.DiscussQuestion("sloan-11", "a-0", "a-1")));
            SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SDSS_LOST_SET, //SSDS Lost Set
                    new SubjectGroup("sloan_tree.xml", false,
                            new DecisionTree.DiscussQuestion("sloan-11", "a-0", "a-1")));
            SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_FERENGI2, //Ferengi 2
                    new SubjectGroup("ferengi_tree.xml", false,
                            new DecisionTree.DiscussQuestion("ferengi-16", "a-0", "a-1")));
            SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_DECALS_DR2, //Decals DR2
                    new SubjectGroup("decals_tree.xml", false,
                            new DecisionTree.DiscussQuestion("decals-11", "a-0", "a-1")));

            //We don't request items for all these groups any more, but we still want to load the
            //trees so can ask questions about items that have already been downloaded and stored in
            //the cache.
            //At some point we can remove some when we are sure they are unnecessary.
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_DECALS, //Decals
            //        new SubjectGroup("decals_tree.xml", false,
            //                new DecisionTree.DiscussQuestion("decals-11", "a-0", "a-1")));
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_ILLUSTRIS, //Illustris
            //        new SubjectGroup("illustris_tree.xml", false,
            //                new DecisionTree.DiscussQuestion("illustris-11", "a-0", "a-1")));
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN, //Sloan
            //        new SubjectGroup("sloan_tree.xml", false,
            //                new DecisionTree.DiscussQuestion("sloan-11", "a-0", "a-1")));
            //SUBJECT_GROUPS.put("551456e02f0eef2535000001", // candels_2epoch
            //        new SubjectGroup("candels_tree.xml", false,
            //                new DecisionTree.DiscussQuestion("candels-17", "a-0", "a-1")));
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_GOODS_FULL, // goods_full
            //        new SubjectGroup("goods_full_tree.xml", false,
            //                new DecisionTree.DiscussQuestion("goods_full-16", "a-0", "a-1")));
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN_SINGLEBAND, // sloan_singleband
            //        new SubjectGroup("sloan_singleband_tree.xml", false,
            //                new DecisionTree.DiscussQuestion("sloan_singleband-11", "a-0", "a-1")));
        }

        //Test:
        /*
        {
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN, // SSID / sloan
            //        new SubjectGroup("sloan_tree.xml", true,
            //                new DecisionTree.DiscussQuestion("sloan-11", "a-0", "a-1")));

            //candels_2epoch is not on dev.zooniverse.org:
            //SUBJECT_GROUPS.put("551456e02f0eef2535000001", // candels_2epoch
            //        new SubjectGroup("candels_tree.xml", true,
            //                new DecisionTree.DiscussQuestion("candels-17", "a-0", "a-1")));

            //goods_full is not on dev.zooniverse.org:
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_GOODS_FULL, // goods_full
            //        new SubjectGroup("goods_full_tree.xml", true,
            //                new DecisionTree.DiscussQuestion("goods_full-16", "a-0", "a-1")));

            //sloan_singlefield is not on dev.zooniverse.org:
            //SUBJECT_GROUPS.put(SUBJECT_GROUP_ID_SLOAN_SINGLEBAND, // sloan_singleband
            //        new SubjectGroup("sloan_singleband_tree.xml", true,
            //                new DecisionTree.DiscussQuestion("sloan_singleband-11", "a-0", "a-1")));

            //Some older surveys, which are on dev.zooniverse.org, just for testing:
            SUBJECT_GROUPS.put("5244909c3ae7402d53000001", // ukidss
                    new SubjectGroup("candels_tree.xml", true,
                            new DecisionTree.DiscussQuestion("candels-17", "a-0", "a-1")));

            SUBJECT_GROUPS.put("5249cbce3ae740728d000001", // ferengi
                    new SubjectGroup("goods_full_tree.xml", true,
                            new DecisionTree.DiscussQuestion("goods_full-16", "a-0", "a-1")));
        }
        */



        SUBJECT_GROUPS_TO_USE_FOR_NEW_QUERIES = new ArrayList<>();
        for (final Map.Entry<String, SubjectGroup> entry : SUBJECT_GROUPS.entrySet()) {
            final Config.SubjectGroup group = entry.getValue();
            if (group.getUseForNewQueries()) {
                SUBJECT_GROUPS_TO_USE_FOR_NEW_QUERIES.add(entry.getKey());
            }
        }
    }
}
