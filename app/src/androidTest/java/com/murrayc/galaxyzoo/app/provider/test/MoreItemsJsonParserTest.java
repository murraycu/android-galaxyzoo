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

import android.support.test.runner.AndroidJUnit4;

import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.client.MoreItemsJsonParser;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MoreItemsJsonParserTest {
    private List<ZooniverseClient.Subject> mSubjects = null;


    @Before
    public void setUp() throws IOException {
        final InputStream inputStream = MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_more_items_response.json");
        assertNotNull(inputStream);

        final Reader reader = new InputStreamReader(inputStream, Utils.STRING_ENCODING);

        mSubjects = MoreItemsJsonParser.parseMoreItemsResponseContent(reader);
        assertNotNull(mSubjects);

        reader.close();
    }

    @Test
    public void testSize() {
        assertEquals(5, mSubjects.size());
    }

    @Test
    public void testValues() {

    }

}
