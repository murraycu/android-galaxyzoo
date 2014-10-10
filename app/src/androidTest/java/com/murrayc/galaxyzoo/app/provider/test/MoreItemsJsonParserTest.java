/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-glom
 *
 * android-glom is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-glom is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-glom.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.provider.test;

import android.test.AndroidTestCase;

import com.murrayc.galaxyzoo.app.provider.client.MoreItemsJsonParser;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class MoreItemsJsonParserTest extends AndroidTestCase {
    private List<ZooniverseClient.Subject> mSubjects = null;

    @Override
    public void setUp() throws IOException {
        final InputStream inputStream = MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_more_items_response.json");
        assertNotNull(inputStream);
        mSubjects = MoreItemsJsonParser.parseMoreItemsResponseContent(inputStream);
        assertNotNull(mSubjects);
    }

    @Override
    public void tearDown() {
    }

    public void testSize() {
        assertEquals(5, mSubjects.size());
    }

    public void testValues() {

    }

}
