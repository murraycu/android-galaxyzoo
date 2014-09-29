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

package com.murrayc.galaxyzoo.app.provider.test;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import java.io.IOException;

/**
 * Simple test to ensure that the generated bindings are working.
 */
public class ContentProviderTest extends ProviderTestCase2<ItemsContentProvider> {

    private static final String VALID_SUBJECT_ID = "SomeSubjectID";
    private MockContentResolver mMockResolver;

    public ContentProviderTest() {
        super(ItemsContentProvider.class, Item.AUTHORITY);
    }

    /**
     * @return a ContentValues object with a value set for each column
     */
    private static ContentValues getFullContentValues() {
        final ContentValues v = new ContentValues(7);
        v.put(Item.Columns.SUBJECT_ID, VALID_SUBJECT_ID);
        return v;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMockResolver = getMockContentResolver();
    }

    public void testInsertUri() {
        final Uri uri = mMockResolver.insert(Item.CONTENT_URI, getFullContentValues());
        assertEquals(1L, ContentUris.parseId(uri));
    }

    public void testInsertThenQueryAll() {
        mMockResolver.insert(Item.CONTENT_URI, getFullContentValues());

        final Cursor cursor = mMockResolver.query(Item.CONTENT_URI, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());

        // TODO: Avoid calling getColumnIndex() when using null to get all fields,
        // because it requires this client code to know the actual column name of the internal database table,
        // because the Cursor won't have the column names mapped.
        assertEquals(VALID_SUBJECT_ID, cursor.getString(cursor.getColumnIndex(Item.Columns.SUBJECT_ID)));
	cursor.close();
    }

    public void testInsertThenQuerySpecific() {
        final Uri uri = mMockResolver.insert(Item.CONTENT_URI, getFullContentValues());

        final Cursor cursor = mMockResolver.query(uri, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());

        // TODO: Avoid calling getColumnIndex() when using null to get all fields,
        // because it requires this client code to know the actual column name of the internal database table,
        // because the Cursor won't have the column names mapped.
        assertEquals(VALID_SUBJECT_ID, cursor.getString(cursor.getColumnIndex(Item.Columns.SUBJECT_ID)));
	cursor.close();
    }


    public void testInsertThenOpenFile() throws IOException {
        final Uri uriItem = mMockResolver.insert(Item.CONTENT_URI, getFullContentValues());
        final Cursor cursor = mMockResolver.query(uriItem, null, null, new String[]{}, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());

        // Get the content: URI for the Item's file:
        // TODO: Avoid calling getColumnIndex() when using null to get all fields,
        // because it requires this client code to know the actual column name of the internal database table,
        // because the Cursor won't have the column names mapped.
        final String fileContentUri = cursor.getString(cursor.getColumnIndex(Item.Columns.LOCATION_STANDARD_URI));
        assertNotNull(fileContentUri);

        //Open the actual file data at that content: URI:
        /* TODO: Test this when we find out how to make getExternalFilesDir() work in this ProviderTestCase2.
        final Uri uri = Uri.parse(fileContentUri);
        final OutputStream stream = mMockResolver.openOutputStream(uri);
        assertNotNull(stream);
        stream.close();
        */

	cursor.close();
    }

    //TODO: Test filtering of mime types?
    public void testGetStreamTypes() {
        final Uri uri = Uri.parse(Item.FILE_URI + "/1");
        final String[] mimeTypes = mMockResolver.getStreamTypes(uri, null);
        assertNotNull(mimeTypes);
        assertEquals(1, mimeTypes.length);
    }

    public void testGetStreamTypesWrongUri() {
        //Only a file uri should provide a data stream:
        try {
            final String[] mimeTypes = mMockResolver.getStreamTypes(Item.CONTENT_URI, null);
            assertNull(mimeTypes);
            fail(); //This should not be reached: The exception should always be thrown.
        } catch (final IllegalArgumentException e) {
        }
    }
}
