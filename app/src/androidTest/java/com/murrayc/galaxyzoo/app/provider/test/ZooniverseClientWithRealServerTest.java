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

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ZooniverseClientWithRealServerTest {
    @Before
    public void setUp() throws IOException {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testMoreItems() throws IOException, ZooniverseClient.RequestMoreItemsException {

        final ZooniverseClient client = createZooniverseClient();

        // Do this enough times that we are very likely to excercise all groups:
        final int count = Config.SUBJECT_GROUPS.size() * 5;
        for (int i = 0; i < count; ++i) {
            final List<ZooniverseClient.Subject> subjects = client.requestMoreItemsSync(1);
            assertNotNull(subjects);
            assertTrue(subjects.size() == 1);

            final ZooniverseClient.Subject subject = subjects.get(0);
            assertNotNull(subject);
            assertFalse(TextUtils.isEmpty(subject.getId()));
        }
    }

    private static ZooniverseClient createZooniverseClient() {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Context context = instrumentation.getTargetContext();
        return new ZooniverseClient(context, Config.SERVER);
    }
}
