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
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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


    public static SharedPreferences getPreferences(final Context context) {
        return context.getSharedPreferences("android-galaxyzoo", Context.MODE_PRIVATE);
    }

    public static boolean getNetworkIsConnected(final Context context) {
        boolean connected = false;
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            connected = true;
        }

        return connected;
    }
}
