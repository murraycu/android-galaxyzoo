/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
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

package com.murrayc.galaxyzoo.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.murrayc.galaxyzoo.app.provider.Item;

//import org.apache.http.client.utils.URIBuilder;

/**
 *
 */
public class Utils {

    //	/** Build the URL for the service that will return the binary data for an image.
//	 *
//	 * @param primaryKeyValue
//	 * @param field
//	 * @return
//	 */
//	public static String buildImageDataUrl(final TypedDataItem primaryKeyValue, final String documentID, final String tableName, final LayoutItemField field) {
//		final URIBuilder uriBuilder = buildImageDataUrlStart(documentID, tableName);
//
//		//TODO: Handle other types:
//		if(primaryKeyValue != null) {
//			uriBuilder.setParameter("value", Double.toString(primaryKeyValue.getNumber()));
//		}
//
//		uriBuilder.setParameter("field", field.getName());
//		return uriBuilder.toString();
//	}
//
//	/** Build the URL for the service that will return the binary data for an image.
//	 *
//	 * @param primaryKeyValue
//	 * @param field
//	 * @return
//	 */
//	public static String buildImageDataUrl(final String documentID, final String tableName, final String layoutName, final int[] path) {
//		final URIBuilder uriBuilder = buildImageDataUrlStart(documentID, tableName);
//		uriBuilder.setParameter("layout", layoutName);
//		uriBuilder.setParameter("layoutpath", buildLayoutPath(path));
//		return uriBuilder.toString();
//	}
//
//	/**
//	 * @param documentID
//	 * @param tableName
//	 * @return
//	 */
//	private static URIBuilder buildImageDataUrlStart(final String documentID, final String tableName) {
//		final URIBuilder uriBuilder = new URIBuilder();
//		//uriBuilder.setHost(GWT.getModuleBaseURL());
//		uriBuilder.setPath("OnlineGlom/gwtGlomImages"); //The name of our images servlet. See OnlineGlomImagesServlet.
//		uriBuilder.setParameter("document", documentID);
//		uriBuilder.setParameter("table", tableName);
//		return uriBuilder;
//	}


    public static Uri buildFileContentUri(final Uri uriItem, final ContentResolver resolver) {
        final String[] projection = new String[]{Item.Columns.LOCATION_STANDARD_URI};
        final Cursor cursor = resolver.query(uriItem, projection, null, new String[]{}, null);
        if (cursor.getCount() <= 0) {
            Log.error("ContentResolver.query() returned no rows.");
            return null;
        }

        cursor.moveToFirst();
        final int index = cursor.getColumnIndex(Item.Columns.LOCATION_STANDARD_URI);
        if (index == -1) {
            Log.error("Cursor.getColumnIndex() failed.");
            return null;
        }

        final String str = cursor.getString(index);
        cursor.close(); //TODO: Should we do this?
        return Uri.parse(str);
    }
}
