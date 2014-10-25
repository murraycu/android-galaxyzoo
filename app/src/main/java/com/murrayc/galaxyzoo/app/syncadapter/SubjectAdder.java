package com.murrayc.galaxyzoo.app.syncadapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;
import com.murrayc.galaxyzoo.app.provider.ImageType;
import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

public class SubjectAdder {
    private final Context mContext;

    /* A map of remote URIs to the last dates that we tried to download them.
     */
    private final Map<String, Date> mImageDownloadsInProgress = new HashMap<>();

    public SubjectAdder(final Context context) {
        this.mContext = context;
    }

    /**
     * Download any images that have previously failed to download.
     *
     * @return Return true if we know for sure that no further downloading is currently necessary.
     */
    boolean downloadMissingImages() {
        boolean noWorkNeeded = true;

        //Get all the items that have an image that is not yet fully downloaded:
        final ContentResolver resolver = getContext().getContentResolver();

        final String[] projection = {Item.Columns._ID,
                Item.Columns.LOCATION_STANDARD_URI_REMOTE,
                Item.Columns.LOCATION_STANDARD_URI,
                Item.Columns.LOCATION_THUMBNAIL_URI_REMOTE,
                Item.Columns.LOCATION_THUMBNAIL_URI,
                Item.Columns.LOCATION_INVERTED_URI_REMOTE,
                Item.Columns.LOCATION_INVERTED_URI};
        final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                getWhereClauseForDownloadNotDone(), new String[]{}, null);

        //Find out if the image is currently being downloaded:
        while (c.moveToNext()) {
            final String itemId = c.getString(0);
            if (TextUtils.isEmpty(itemId)) {
                continue;
            }

            final Uri itemUri = Utils.getItemUri(itemId);

            //Restart any downloads that seem to have failed before, or have been interrupted:
            final String uriStandardRemote = c.getString(1);
            if (!mImageDownloadsInProgress.containsKey(uriStandardRemote)) {
                final String uriStandard = c.getString(2);
                downloadMissingImage(itemUri, uriStandardRemote, uriStandard, ImageType.STANDARD);
                noWorkNeeded = false;
            }

            final String uriThumbnailRemote = c.getString(3);
            if (!mImageDownloadsInProgress.containsKey(uriThumbnailRemote)) {
                final String uriThumbnail = c.getString(4);
                downloadMissingImage(itemUri, uriThumbnailRemote, uriThumbnail, ImageType.THUMBNAIL);
                noWorkNeeded = false;
            }

            final String uriInvertedRemote = c.getString(5);
            if (!mImageDownloadsInProgress.containsKey(uriThumbnailRemote)) {
                final String uriInverted = c.getString(6);
                downloadMissingImage(itemUri, uriInvertedRemote, uriInverted, ImageType.INVERTED);
                noWorkNeeded = false;
            }
        }

        c.close();

        return noWorkNeeded;
    }

    private void downloadMissingImage(final Uri itemUri, final String uriRemote, final String uriContent, ImageType imageType) {
        try {
            cacheUriToFile(uriRemote, uriContent, itemUri, imageType, true /* async */);
        } catch (final HttpUtils.NoNetworkException e) {
            Log.info("downloadMissingImage(): No network connection.");
        }
    }

    /**
     * @param subjects
     * @param asyncFileDownloads Get the image data asynchronously if this is true.
     */
    public void addSubjects(final List<ZooniverseClient.Subject> subjects, boolean asyncFileDownloads) {
        if (subjects == null) {
            return;
        }

        for (final ZooniverseClient.Subject subject : subjects) {
            addSubject(subject, asyncFileDownloads);
        }
    }


    private void cacheUrisToFiles(final Uri itemUri, boolean asyncFileDownloads) {

        final ContentResolver resolver = getContext().getContentResolver();

        //Actually cache the URIs' data in the local files:
        //This will mark the data as fully downloaded by setting the *Downloaded boolean fields,
        //so we do this only after creating the items record.

        final String[] projection = {
                Item.Columns.LOCATION_STANDARD_URI_REMOTE,
                Item.Columns.LOCATION_STANDARD_URI,
                Item.Columns.LOCATION_THUMBNAIL_URI_REMOTE,
                Item.Columns.LOCATION_THUMBNAIL_URI,
                Item.Columns.LOCATION_INVERTED_URI_REMOTE,
                Item.Columns.LOCATION_INVERTED_URI,
        };
        final Cursor c = resolver.query(itemUri, projection,
                null, new String[]{}, null);
        while (c.moveToNext()) {
            final String uriStandardRemote = c.getString(0);
            final String uriStandard = c.getString(1);
            final String uriThumbnailRemote = c.getString(2);
            final String uriThumbnail = c.getString(3);
            final String uriInvertedRemote = c.getString(4);
            final String uriInverted = c.getString(5);

            if (!(cacheUriToFile(uriStandardRemote, uriStandard, itemUri, ImageType.STANDARD, asyncFileDownloads))) {
                Log.error("cacheUrisToFiles(): cacheUriToFile() failed for standard image.");
            }

            if (!(cacheUriToFile(uriThumbnailRemote, uriThumbnail, itemUri, ImageType.THUMBNAIL, asyncFileDownloads))) {
                Log.error("cacheUrisToFiles(): cacheUriToFile() failed for thumbnail image.");
            }

            if (!(cacheUriToFile(uriInvertedRemote, uriInverted, itemUri, ImageType.INVERTED, asyncFileDownloads))) {
                Log.error("cacheUrisToFiles(): cacheUriToFile() failed for inverted image.");
            }
        }

        c.close();
    }

    /**
     * Download bytes from a url and store them in a file, optionally asynchronously in spawned thread.
     *
     * @param asyncFileDownloads Get the image data asynchronously if this is true.
     */
    boolean cacheUriToFile(final String uriFileToCache, final String cacheFileUri, final Uri itemUri, final ImageType imageType, boolean asyncFileDownloads) {
        if (TextUtils.isEmpty(uriFileToCache)) {
            return false;
        }

        if (TextUtils.isEmpty(cacheFileUri)) {
            return false;
        }

        //Don't attempt it if it is already in progress.
        if (mImageDownloadsInProgress.containsKey(uriFileToCache)) {
            //TODO: Check the actual date?
            return false;
        }

        throwIfNoNetwork();

        final Date now = new Date();
        mImageDownloadsInProgress.put(uriFileToCache, now);

        if (asyncFileDownloads) {
            final FileCacheAsyncTask task = new FileCacheAsyncTask(this, itemUri, imageType);
            try {
                task.execute(uriFileToCache, cacheFileUri);
            } catch (final RejectedExecutionException e) {
                //This happened once because >128 were running.
                Log.error("cacheUriToFile(): Couldn't execute FileCacheAsyncTask()", e);
                return false;
            }

            return true;
        } else {
            final boolean downloaded = HttpUtils.cacheUriToContentUriFileSync(getContext(), uriFileToCache, cacheFileUri);
            markImageDownloadAsNotInProgress(uriFileToCache);
            if (downloaded) {
                return markImageAsDownloaded(itemUri, imageType, uriFileToCache);
            } else {
                //doRegularTasks() will try again later.
                Log.error("cacheUriToFile(): cacheUriToContentUriFileSync(): failed.");
                return false;
            }
        }
    }

    private Context getContext() {
        return mContext;
    }

    private void throwIfNoNetwork() {
        HttpUtils.throwIfNoNetwork(getContext());
    }


    private void markImageDownloadAsNotInProgress(final String uriFileToCache) {
        mImageDownloadsInProgress.remove(uriFileToCache);
    }

    boolean markImageAsDownloaded(final Uri itemUri, final ImageType imageType, final String uriFileToCache) {

        //Don't try downloading this again later:

        //Actually the caller should already have removed this,
        //regardless of the download's success or failure.
        //but let's be sure:
        markImageDownloadAsNotInProgress(uriFileToCache);

        //Let users of the ContentProvider API know that the image has been fully downloaded
        //so it's safe to use it:
        String fieldName = null;
        switch (imageType) {
            case STANDARD:
                fieldName = Item.Columns.LOCATION_STANDARD_DOWNLOADED;
                break;
            case THUMBNAIL:
                fieldName = Item.Columns.LOCATION_THUMBNAIL_DOWNLOADED;
                break;
            case INVERTED:
                fieldName = Item.Columns.LOCATION_INVERTED_DOWNLOADED;
                break;
            default:
                Log.error("markImageAsDownloaded(): Unexpected imageType.");
                return false;
        }

        final ContentResolver resolver = getContext().getContentResolver();

        final ContentValues values = new ContentValues();
        values.put(fieldName, 1);

        final int affected = resolver.update(itemUri, values,
                null, null);
        if (affected != 1) {
            Log.error("markImageAsDownloaded(): Failed to mark image download as done.");
            return false;
        } else {
            //Let the ListView (or other UI) know that there is more to display.
            //TODO? notifyRowChangeBySubjectId(subjectId);
        }

        return true;
    }

    /**
     * @param item
     * @param asyncFileDownloads Get the image data asynchronously if this is true.
     */
    void addSubject(final ZooniverseClient.Subject item, boolean asyncFileDownloads) {
        if (subjectIsInDatabase(item.getSubjectId())) {
            //It is already in the database.
            //TODO: Update the row?
            return;
        }

        final ContentResolver resolver = getContext().getContentResolver();

        final ContentValues values = new ContentValues();
        values.put(Item.Columns.SUBJECT_ID, item.getSubjectId());
        values.put(Item.Columns.ZOONIVERSE_ID, item.getZooniverseId());

        //The ItemsContentProvider will take care of creating local file URIs for the remote URis,
        //and this SyncAdapter will request that the remote image files are downloaded into those local file URIs.
        values.put(Item.Columns.LOCATION_STANDARD_URI_REMOTE, item.getLocationStandard());
        values.put(Item.Columns.LOCATION_THUMBNAIL_URI_REMOTE, item.getLocationThumbnail());
        values.put(Item.Columns.LOCATION_INVERTED_URI_REMOTE, item.getLocationInverted());

        final Uri itemUri = resolver.insert(Item.ITEMS_URI, values);
        if (itemUri == null) {
            throw new IllegalStateException("could not insert " +
                    "content values: " + values);
        }

        cacheUrisToFiles(itemUri, asyncFileDownloads);

        //TODO: notifyRowChangeById(rowId);
    }

    boolean subjectIsInDatabase(final String subjectId) {
        //TODO: Use COUNT_AS_COUNT ?
        final ContentResolver resolver = getContext().getContentResolver();

        final String[] projection = {Item.Columns.SUBJECT_ID};
        final String whereClause = Item.Columns.SUBJECT_ID + " = ?"; //We use ? to avoid SQL Injection.
        final String[] selectionArgs = {subjectId};
        final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                whereClause, selectionArgs, null);
        final boolean result = c.getCount() > 0;
        c.close();
        return result;
    }

    static class FileCacheAsyncTask extends AsyncTask<String, Integer, Boolean> {

        private final Uri itemUri;
        private final ImageType imageType;
        private String uriFileToCache = null;
        private final WeakReference<SubjectAdder> parentReference;

        public FileCacheAsyncTask(final SubjectAdder parent, final Uri itemUri, ImageType imageType) {
            this.itemUri = itemUri;
            this.imageType = imageType;
            this.parentReference = new WeakReference<>(parent);
        }

        @Override
        protected Boolean doInBackground(final String... params) {
            if (params.length < 2) {
                Log.error("FileCacheAsyncTask: not enough params.");
                return false;
            }

            Log.info("FileCacheAsyncTask.doInBackground()");

            //TODO: Just set these in the constructor?
            uriFileToCache = params[0];
            final String cacheFileUri = params[1];

            final SubjectAdder parent = getParent();
            if (parent == null) {
                Log.error("FileCacheAsyncTask(): doInBackground(): parent is null.");
                return false;
            }

            return HttpUtils.cacheUriToContentUriFileSync(parent.getContext(), uriFileToCache, cacheFileUri);
        }

        private SubjectAdder getParent() {
            if (parentReference == null) {
                return null;
            }

            return parentReference.get();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (parentReference == null) {
                Log.error("FileCacheAsyncTask.onPostExcecute(): parentReference is null.");
                return;
            }

            final SubjectAdder parent = parentReference.get();
            if (parent == null) {
                Log.error("FileCacheAsyncTask.onPostExcecute(): parent is null.");
                return;
            }

            parent.markImageDownloadAsNotInProgress(uriFileToCache);

            if (result) {
                if (!parent.markImageAsDownloaded(itemUri, imageType, uriFileToCache)) {
                    Log.error("FileCacheAsyncTask(): onPostExecute(): markImageAsDownloaded() failed.");
                }
            } else {
                //doRegularTasks() will try again later.
                Log.error("FileCacheAsyncTask(): cacheUriToContentUriFileSync(): failed.");
            }

            super.onPostExecute(result);
        }
    }

    private static String getWhereClauseForDownloadNotDone() {
        return "(" +
                Item.Columns.LOCATION_STANDARD_DOWNLOADED + " != 1" +
                ") OR (" +
                Item.Columns.LOCATION_THUMBNAIL_DOWNLOADED + " != 1" +
                ") OR (" +
                Item.Columns.LOCATION_INVERTED_DOWNLOADED + " != 1" +
                ")";
    }
}
