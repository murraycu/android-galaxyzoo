/*
 * Copyright (C) 2018 Murray Cumming
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.murrayc.galaxyzoo.app.Utils;
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
public class DecisionTreeJsonParserTest {
    private List<ZooniverseClient.Workflow> mWorkflows = null;

    @Before
    public void setUp() throws IOException {
        final InputStream inputStream = MoreItemsJsonParserTest.class.getClassLoader().getResourceAsStream("test_decision_tree.json");
        assertNotNull(inputStream);

        final Reader reader = new InputStreamReader(inputStream, Utils.STRING_ENCODING);

        final Gson gson = ZooniverseClient.createGson();
        final ZooniverseClient.WorkflowsResponse response = gson.fromJson(reader, new TypeToken<ZooniverseClient.WorkflowsResponse>() {}.getType());
        assertNotNull(response);

        mWorkflows = response.workflows;
        assertNotNull(mWorkflows);

        reader.close();
    }

    @Test
    public void testSize() {
        assertEquals(1, mWorkflows.size());
    }

    @Test
    public void testValues() {
        final ZooniverseClient.Workflow workflow = mWorkflows.get(0);
        assertNotNull(workflow);
        assertEquals("6122", workflow.id());
        assertEquals("DECaLS DR5", workflow.displayName());


        final List<ZooniverseClient.Task> tasks = workflow.tasks();
        assertNotNull(tasks);
        assertEquals(11, tasks.size());

        final ZooniverseClient.Task task = tasks.get(0);
        assertNotNull(task);
        assertEquals("T0", task.id());
        assertEquals("single", task.type());
        assertEquals("Is the galaxy simply smooth and rounded, with no sign of a disk?", task.question());
        assertNotNull(task.help()); // TODO: Validate the markdown?
        assertEquals(true, task.required());

        final List<ZooniverseClient.Answer> answers = task.answers();
        assertNotNull(answers);
        assertEquals(3, answers.size());
    }
}
