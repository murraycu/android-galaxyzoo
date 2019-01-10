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
public class ProjectsJsonParserTest {
    private List<ZooniverseClient.Project> mProjects = null;

    @Before
    public void setUp() throws IOException {
        final InputStream inputStream = ProjectsJsonParserTest.class.getClassLoader().getResourceAsStream("test_project_response.json");
        assertNotNull(inputStream);

        final Reader reader = new InputStreamReader(inputStream, Utils.STRING_ENCODING);

        final Gson gson = ZooniverseClient.createGson();
        final ZooniverseClient.ProjectsResponse response = gson.fromJson(reader, new TypeToken<ZooniverseClient.ProjectsResponse>() {}.getType());
        assertNotNull(response);

        mProjects = response.projects;
        assertNotNull(mProjects);

        reader.close();
    }

    @Test
    public void testSize() {
        assertEquals(1, mProjects.size());
    }

    @Test
    public void testValues() {
        final ZooniverseClient.Project project = mProjects.get(0);
        assertNotNull(project);

        assertNotNull(project.id());
        assertEquals("5733", project.id());

        assertNotNull(project.displayName());
        assertEquals("Galaxy Zoo", project.displayName());

        final List<String> workflowIds = project.workflowIds();
        assertNotNull(workflowIds);
        assertEquals(5, workflowIds.size());

        final List<String> activeWorkflowIds = project.activeWorkflowIds();
        assertNotNull(activeWorkflowIds);
        assertEquals(1, activeWorkflowIds.size());

        final String activeWorkflowID = activeWorkflowIds.get(0);
        assertNotNull(activeWorkflowID);
        assertEquals("6122", activeWorkflowID);

    }

}
