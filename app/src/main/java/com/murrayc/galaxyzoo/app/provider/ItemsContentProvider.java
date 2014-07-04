/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.provider;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.Singleton;
import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.provider.rest.FileResponseHandler;
import com.murrayc.galaxyzoo.app.provider.rest.GalaxyZooResponseHandler;
import com.murrayc.galaxyzoo.app.provider.rest.UriRequestTask;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsContentProvider extends ContentProvider {

    public static final String METHOD_REQUEST_ITEMS = "request-items";
    public static final String URI_PART_ITEM = "item";
    public static final String URI_PART_FILE = "file";

    /**
     * The MIME type of {@link Item#CONTENT_URI} providing a directory of notes.
     */
    private static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.android-galaxyzoo.item";

    /**
     * The MIME type of a {@link Item#CONTENT_URI} sub-directory of a single
     * item.
     */
    private static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.android-galaxyzoo.item";

    /** REST uri for querying items. */
    private static final String QUERY_URI =
            "https://api.zooniverse.org/projects/galaxy_zoo/recents";


    //TODO: Use an enum?
    private static final int MATCHER_ID_ITEMS = 1;
    private static final int MATCHER_ID_ITEM = 2;
    private static final int MATCHER_ID_FILE = 3;
    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // A URI for the list of all items:
        sUriMatcher.addURI(Item.AUTHORITY, URI_PART_ITEM, MATCHER_ID_ITEMS);

        // A URI for a single item:
        sUriMatcher.addURI(Item.AUTHORITY, URI_PART_ITEM + "/#", MATCHER_ID_ITEM);

        // A URI for a single file:
        sUriMatcher.addURI(Item.AUTHORITY, URI_PART_FILE + "/#", MATCHER_ID_FILE);
    }

    private static final String[] FILE_MIME_TYPES = new String[]{"application/x-glom"};


    /**
     * A map of GlomContentProvider projection column names to underlying Sqlite column names
     * for /item/ URIs, mapping to the items tables.
     */
    private static final Map<String, String> sItemsProjectionMap;

    static {
        sItemsProjectionMap = new HashMap<>();

        sItemsProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
        sItemsProjectionMap.put(Item.Columns.SUBJECT_ID, DatabaseHelper.DB_COLUMN_NAME_SUBJECT_ID);
        sItemsProjectionMap.put(Item.Columns.ZOONIVERSE_ID, DatabaseHelper.DB_COLUMN_NAME_ZOONIVERSE_ID);
        sItemsProjectionMap.put(Item.Columns.LOCATION_STANDARD_URI, DatabaseHelper.DB_COLUMN_NAME_LOCATION_STANDARD_URI);
        sItemsProjectionMap.put(Item.Columns.LOCATION_THUMBNAIL_URI, DatabaseHelper.DB_COLUMN_NAME_LOCATION_THUMBNAIL_URI);
        sItemsProjectionMap.put(Item.Columns.LOCATION_INVERTED_URI, DatabaseHelper.DB_COLUMN_NAME_LOCATION_INVERTED_URI);
    }


    private final Singleton documentSingleton = Singleton.getInstance();
    private DatabaseHelper mOpenDbHelper;

    public ItemsContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int affected;

        switch (match) {
            //TODO: Do not support this because it would delete everything in one go?
            case MATCHER_ID_ITEMS:
                affected = getDb().delete(DatabaseHelper.TABLE_NAME_ITEMS,
                        (!TextUtils.isEmpty(selection) ?
                                " AND (" + selection + ')' : ""),
                        selectionArgs
                );
                //TODO: Delete all associated files too.
                break;
            case MATCHER_ID_ITEM:
                final UriParts uriParts = parseContentUri(uri);
                affected = getDb().delete(DatabaseHelper.TABLE_NAME_ITEMS,
                        prependIdToSelection(selection),
                        prependToArray(selectionArgs, uriParts.itemId)
                );
                //TODO: Delete the associated files too.
                break;
            //TODO?: case MATCHER_ID_FILE:
            default:
                throw new IllegalArgumentException("unknown item: " +
                        uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return affected;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case MATCHER_ID_ITEMS:
                return CONTENT_TYPE;
            case MATCHER_ID_ITEM:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown item type: " +
                        uri);
        }
    }

    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {

        switch (sUriMatcher.match(uri)) {
            case MATCHER_ID_FILE:
                if (mimeTypeFilter != null) {
                    // We use ClipDescription just so we can use its filterMimeTypes()
                    // though we are not intested in ClipData here.
                    // TODO: Find a more suitable utility function?
                    final ClipDescription clip = new ClipDescription(null, FILE_MIME_TYPES);
                    return clip.filterMimeTypes(mimeTypeFilter);
                } else {
                    return FILE_MIME_TYPES;
                }
            default:
                throw new IllegalArgumentException("Unknown type: " +
                        uri);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        return super.openFileHelper(uri, mode);
    }

    //TODO: Is this actually used by anything?
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != MATCHER_ID_ITEMS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Cope with a null values:
        ContentValues valuesToUse;
        if (values != null) {
            valuesToUse = new ContentValues(values);
        } else {
            valuesToUse = new ContentValues();
        }

        //TODO: verifyValues(values);

        // insert the initialValues into a new database row
        final SQLiteDatabase db = getDb();

        final Subject subject = new Subject();
        //TODO:.

        final long rowId = addSubject(subject);
        if (rowId >= 0) {
            final Uri itemUri = ContentUris.withAppendedId(
                    Item.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(itemUri, null);
            return itemUri; //The URI of the newly-added Item.
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    private Uri createFileUri(final String uriOfFileToCache) {
        final SQLiteDatabase db = getDb();
        final long fileId = db.insertOrThrow(DatabaseHelper.TABLE_NAME_FILES,
                DatabaseHelper.DB_COLUMN_NAME_FILE_DATA, null);

        //Build a value for the _data column, using the autogenerated file _id:
        String realFileUri = "";
        try {
            final Context context = getContext();
            if (context != null) {
                final File realFile = new File(context.getExternalFilesDir(null),
                        Long.toString(fileId)); //TODO: Is toString() affected by the locale?

                //Actually create an empty file there -
                //otherwise when we try to write to it via openOutputStream()
                //we will get a FileNotFoundException.
                realFile.createNewFile();

                realFileUri = realFile.getAbsolutePath();
            }
        } catch (UnsupportedOperationException e) {
            //This happens while running under ProviderTestCase2.
            //so we just catch it and provide a useful value,
            //so at least the other functionality can be tested.
            //TODO: Find a way to let it succeed.
            realFileUri = "testuri";
            Log.error("Unsupported operation", e);
        } catch (IOException e) {
            Log.error("IOException", e);
            return null;
        }

        //Put the value for the _data column in the files table:
        //This will be used implicitly by openOutputStream() and openInputStream():
        ContentValues valuesUpdate = new ContentValues();
        valuesUpdate.put(DatabaseHelper.DB_COLUMN_NAME_FILE_DATA, realFileUri);
        db.update(DatabaseHelper.TABLE_NAME_FILES, valuesUpdate,
                BaseColumns._ID + " = ?", new String[]{Double.toString(fileId)});

        //Build the content: URI for the file to put in the Item's table:
        Uri fileUri = null;
        if (fileId >= 0) {
            fileUri = ContentUris.withAppendedId(Item.FILE_URI, fileId);
            //TODO? getContext().getContentResolver().notifyChange(fileId, null);
        }

        //Actually cache the URI's data in the local file:
        cacheUriToFile(uriOfFileToCache, realFileUri);


        return fileUri;
    }


    /**
     * Spawns a thread to download bytes from a url and store them in a file.
     */
    private void cacheUriToFile(final String uriFileToCache, final String cacheFileUri) {
        // use id as a unique request tag
        final HttpGet get = new HttpGet(uriFileToCache);
        final UriRequestTask requestTask = new UriRequestTask(
                get, new FileResponseHandler(cacheFileUri),
                getContext());
        final Thread t = new Thread(requestTask);
        t.start();
    }

    @Override
    public boolean onCreate() {
        mOpenDbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!METHOD_REQUEST_ITEMS.equals(method)) {
            return null;
        }

        /** Check with the remote REST API asynchronously,
         * informing the calling client later via notification.
         */
        //TODO: Only do this if there are no unclassified items:
        asyncQueryRequest(QUERY_URI);

        return null;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        //TODO: Avoid a direct implicit mapping between the Cursor column names in "selection" and the
        //underlying SQL database names.

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = DatabaseHelper.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        int match = sUriMatcher.match(uri);

        Cursor c;
        switch (match) {
            case MATCHER_ID_ITEMS: {
                // query the database for all items:
                final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(DatabaseHelper.TABLE_NAME_ITEMS);
                builder.setProjectionMap(sItemsProjectionMap);
                c = builder.query(getDb(), projection,
                        selection, selectionArgs,
                        null, null, orderBy);

                c.setNotificationUri(getContext().getContentResolver(),
                        Item.CONTENT_URI);

                //The client must call(TODO) sometime to actually fill the database with items,
                //and the client will then be notified via the cursor that there are new items.

                break;
            }
            case MATCHER_ID_ITEM: {
                // query the database for a specific item:
                final UriParts uriParts = parseContentUri(uri);

                //Prepend our ID=? argument to the selection arguments.
                //This lets us use the ? syntax to avoid SQL injection

                final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(DatabaseHelper.TABLE_NAME_ITEMS);
                builder.setProjectionMap(sItemsProjectionMap);
                builder.appendWhere(BaseColumns._ID + " = ?"); //We use ? to avoid SQL Injection.
                c = builder.query(getDb(), projection,
                        selection, prependToArray(selectionArgs, uriParts.itemId),
                        null, null, orderBy);
                c.setNotificationUri(getContext().getContentResolver(),
                        Item.CONTENT_URI); //TODO: More precise?
                break;
            }

            case MATCHER_ID_FILE:
                // query the database for a specific file:
                // The caller will then use the _data value (the normal filesystem URI of a file).
                final long fileId = ContentUris.parseId(uri);

                //Prepend our ID=? argument to the selection arguments.
                //This lets us use the ? syntax to avoid SQL injection
                c = getDb().query(DatabaseHelper.TABLE_NAME_FILES, projection,
                        prependIdToSelection(selection),
                        prependToArray(selectionArgs, fileId), null, null, orderBy
                );

                c.setNotificationUri(getContext().getContentResolver(),
                        Item.FILE_URI); //TODO: More precise?
                break;
            default:
                //This could be because of an invalid -1 ID in the # position.
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }

        //TODO: Can we avoid passing a Sqlite cursor up as a ContentResolver cursor?
        return c;
    }

    private String[] prependToArray(final String[] selectionArgs, long value) {
        return prependToArray(selectionArgs, Double.toString(value));
    }

    private String[] prependToArray(final String[] array, final String value) {
        int arrayLength = 0;
        if (array != null) {
            arrayLength = array.length;
        }

        final String[] result = new String[arrayLength + 1];
        result[0] = value;

        if (arrayLength > 0) {
            System.arraycopy(array, 0, result, 1, result.length);
        }

        return result;
    }

    private UriParts parseContentUri(final Uri uri) {
        final UriParts result = new UriParts();
        //ContentUris.parseId(uri) gets the first ID, not the last.
        //final long userId = ContentUris.parseId(uri);
        final List<String> uriParts = uri.getPathSegments();
        final int size = uriParts.size();

        if (size < 2) {
            Log.error("The URI did not have the expected number of parts.");
        }

        //Note: The UriMatcher will not even match the URI if this id (#) is -1
        //so we will never reach this code then:
        result.itemId = uriParts.get(1);

        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int affected;

        switch (sUriMatcher.match(uri)) {
            case MATCHER_ID_ITEMS:
                affected = getDb().update(DatabaseHelper.TABLE_NAME_ITEMS, values,
                        selection, selectionArgs);
                break;

            case MATCHER_ID_ITEM:
                final UriParts uriParts = parseContentUri(uri);

                //Prepend our ID=? argument to the selection arguments.
                //This lets us use the ? syntax to avoid SQL injection
                affected = getDb().update(DatabaseHelper.TABLE_NAME_ITEMS, values,
                        prependIdToSelection(selection),
                        prependToArray(selectionArgs, uriParts.itemId)
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return affected;
    }

    private String prependIdToSelection(final String selection) {
        return BaseColumns._ID + " = ?"
                + (!TextUtils.isEmpty(selection) ?
                " AND (" + selection + ')' : "");
    }

    private SQLiteDatabase getDb() {
        return mOpenDbHelper.getWritableDatabase();
    }

    public long addSubject(final Subject item) {
        if(subjectIsInDatabase(item.mId)) {
            //It is already in the database.
            //TODO: Update the row?
            return 0;
        }

        final SQLiteDatabase db = getDb();

        final ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DB_COLUMN_NAME_SUBJECT_ID, item.mId);
        values.put(DatabaseHelper.DB_COLUMN_NAME_ZOONIVERSE_ID, item.mZooniverseId);

        //Get (our) local content URIs of cache files instead of the remote URIs for the images:
        Uri fileUri = createFileUri(item.mLocationStandard);
        if (fileUri != null) {
            values.put(DatabaseHelper.DB_COLUMN_NAME_LOCATION_STANDARD_URI, fileUri.toString());
        }

        fileUri = createFileUri(item.mLocationThumbnail);
        if (fileUri != null) {
            values.put(DatabaseHelper.DB_COLUMN_NAME_LOCATION_THUMBNAIL_URI, fileUri.toString());
        }

        fileUri = createFileUri(item.mLocationInverted);
        if (fileUri != null) {
            values.put(DatabaseHelper.DB_COLUMN_NAME_LOCATION_INVERTED_URI, fileUri.toString());
        }

        final long rowId = db.insert(DatabaseHelper.TABLE_NAME_ITEMS,
                Item.Columns._ID, values);
        if (rowId >= 0) {
            final Uri insertUri =
                    ContentUris.withAppendedId(
                            Item.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(insertUri, null);
            return rowId;
        } else {
            throw new IllegalStateException("could not insert " +
                    "content values: " + values);
        }
    }

    private boolean subjectIsInDatabase(final String subjectId) {
        final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DatabaseHelper.TABLE_NAME_ITEMS);
        builder.setProjectionMap(sItemsProjectionMap);
        builder.appendWhere(Item.Columns.SUBJECT_ID + " = ?"); //We use ? to avoid SQL Injection.
        final String[] projection = {Item.Columns.SUBJECT_ID};
        final String[] selectionArgs = {subjectId};
        final Cursor c = builder.query(getDb(), projection,
                null, selectionArgs,
                null, null, null);
        return c.getCount() > 0;
    }

    /**
     * There are 2 tables: items and files.
     * The items table has a uri field that specifies a record in the files tables.
     * The files table has a (standard for openInput/OutputStream()) _data field that
     * contains the URI of the file for the item.
     * <p/>
     * The location and creation of the SQLite database is left entirely up to the SQLiteOpenHelper
     * class. We just store its name in the Document.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        protected static final String DB_COLUMN_NAME_SUBJECT_ID = "subjectId";
        protected static final String DB_COLUMN_NAME_ZOONIVERSE_ID = "zooniverseId";
        protected static final String DB_COLUMN_NAME_LOCATION_STANDARD_URI = "locationStandardUri"; //The content URI for a file in the files table.
        protected static final String DB_COLUMN_NAME_LOCATION_THUMBNAIL_URI = "locationThumbnailUri"; //The content URI for a file in the files table.
        protected static final String DB_COLUMN_NAME_LOCATION_INVERTED_URI = "locationInvertedUri"; //The content URI for a file in the files table.
        private static final String DATABASE_NAME = "items.db";

        private static final int DATABASE_VERSION = 3;

        private static final String TABLE_NAME_ITEMS = "items";
        private static final String TABLE_NAME_FILES = "files";
        private static final String DB_COLUMN_NAME_FILE_DATA = "_data"; //The real URI

        private static final String DEFAULT_SORT_ORDER = Item.Columns._ID + " DESC";

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            createTable(sqLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase,
                              int oldv, int newv) {
            //TODO: Don't just lose the data?
            if (oldv != newv) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " +
                        TABLE_NAME_ITEMS + ";");
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " +
                        TABLE_NAME_FILES+ ";");
                createTable(sqLiteDatabase);
            }
        }

        private void createTable(SQLiteDatabase sqLiteDatabase) {
            String qs = "CREATE TABLE " + TABLE_NAME_ITEMS + " (" +
                    BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DB_COLUMN_NAME_SUBJECT_ID + " TEXT, " +
                    DB_COLUMN_NAME_ZOONIVERSE_ID + " TEXT, " +
                    DB_COLUMN_NAME_LOCATION_STANDARD_URI + " TEXT, " +
                    DB_COLUMN_NAME_LOCATION_THUMBNAIL_URI + " TEXT, " +
                    DB_COLUMN_NAME_LOCATION_INVERTED_URI + " TEXT)";
            sqLiteDatabase.execSQL(qs);

            qs = "CREATE TABLE " + TABLE_NAME_FILES + " (" +
                    BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DB_COLUMN_NAME_FILE_DATA + " TEXT);";
            sqLiteDatabase.execSQL(qs);
        }
    }

    public static class Subject {
        public String mId;
        public String mZooniverseId;
        public String mLocationStandard;
        public String mLocationThumbnail;
        public String mLocationInverted;
    }

    private class UriParts {
        public String itemId;
    }

    /*
    private File getFile(long id) {
        return new File(getContext().getExternalFilesDir(null), Long
                .toString(id)
                + ".glom");
    }
    */


    /**
     * Creates a new worker thread to carry out a RESTful network invocation.
     *
     * @param queryUri the complete URI that should be accessed by this request.
     */
    private void asyncQueryRequest(final String queryUri) {
        //synchronized (mRequestsInProgress) {
            UriRequestTask requestTask = null; //getRequestTask();
            if (requestTask == null) {
                requestTask = newQueryTask(queryUri);
                Thread t = new Thread(requestTask);
                // allows other requests to run in parallel.
                t.start();
            }
        //}
    }

    UriRequestTask newQueryTask(final String url) {
        UriRequestTask requestTask;

        final HttpGet get = new HttpGet(url);
        ResponseHandler handler = newResponseHandler();
        requestTask = new UriRequestTask("" /* TODO */, this, get,
                handler, getContext());

        //mRequestsInProgress.put(requestTag, requestTask);
        return requestTask;
    }

    /**
     * Abstract method that allows a subclass to define the type of handler
     * that should be used to parse the response of a given request.
     *
     * @return The response handler created by a subclass used to parse the
     *         request response.
     */
    protected ResponseHandler newResponseHandler() {
        return new GalaxyZooResponseHandler(this);
    }
}
