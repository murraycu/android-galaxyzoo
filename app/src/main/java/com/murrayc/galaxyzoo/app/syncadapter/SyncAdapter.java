package com.murrayc.galaxyzoo.app.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.R;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.ClassificationAnswer;
import com.murrayc.galaxyzoo.app.provider.ClassificationCheckbox;
import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;
import com.murrayc.galaxyzoo.app.provider.ImageType;
import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by murrayc on 10/4/14.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String COUNT_AS_COUNT = "COUNT(*) AS count";
    private int mUploadsInProgress = 0;

    private boolean mRequestMoreItemsAsyncInProgress = false;

    /* A map of remote URIs to the last dates that we tried to download them.
     */
    private final Map<String, Date> mImageDownloadsInProgress = new HashMap<>();

    private ZooniverseClient mClient = null;

    public SyncAdapter(final Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mClient = new ZooniverseClient(context, Config.SERVER);
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider, SyncResult syncResult) {
        doRegularTasks();
    }

    /**
     * Do any uploads, downloads, or removals that are currently necessary.
     * This might not finish all necessary work, so subsequent calls might be necessary.
     *
     * @return Return true if we know for sure that no further work is currently necessary.
     */
    private boolean doRegularTasks() {
        Log.info("doRegularTasks() start");
        //Do the download first, to avoid the UI having to wait for new subjects to classify.
        final boolean noDownloadNecessary = downloadMinimumSubjectsAsync();
        final boolean noDownloadMissingImagesNecessary = downloadMissingImages();

        //Do less urgent things next:
        final boolean noUploadNecessary = uploadOutstandingClassifications();
        final boolean noRemovalNecessary = removeOldSubjects();


        Log.info("doRegularTasks() end");

        return noDownloadNecessary && noDownloadMissingImagesNecessary && noUploadNecessary
                && noRemovalNecessary;
    }

    /**
     * Download any images that have previously failed to download.
     *
     * @return Return true if we know for sure that no further downloading is currently necessary.
     */
    private boolean downloadMissingImages() {
        boolean noWorkNeeded = true;

        //Get all the items that have an image that is not yet fully downloaded:
        final ContentResolver resolver = getContentResolver();

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

            final Uri itemUri = getItemUri(itemId);

            //Restart any downloads that seem to have failed before, or have been interrupted:
            final String uriStandardRemote = c.getString(1);
            if (!mImageDownloadsInProgress.containsKey(uriStandardRemote)) {
                final String uriStandard = c.getString(2);
                downloadMissingImage(uriStandardRemote, uriStandard, itemUri, ImageType.STANDARD);
                noWorkNeeded = false;
            }

            final String uriThumbnailRemote = c.getString(3);
            if (!mImageDownloadsInProgress.containsKey(uriThumbnailRemote)) {
                final String uriThumbnail = c.getString(4);
                downloadMissingImage(uriThumbnailRemote, uriThumbnail, itemUri, ImageType.THUMBNAIL);
                noWorkNeeded = false;
            }

            final String uriInvertedRemote = c.getString(5);
            if (!mImageDownloadsInProgress.containsKey(uriThumbnailRemote)) {
                final String uriInverted = c.getString(6);
                downloadMissingImage(uriInvertedRemote, uriInverted, itemUri, ImageType.INVERTED);
                noWorkNeeded = false;
            }
        }

        c.close();

        return noWorkNeeded;
    }

    private void downloadMissingImage(final String uriRemote, final String uriContent, final Uri itemUri, ImageType imageType) {
        try {
            cacheUriToFile(uriRemote, uriContent, itemUri, imageType, true /* async */);
        } catch (final HttpUtils.NoNetworkException e) {
            Log.info("downloadMissingImages(): No network connection.");
        }
    }

    private ContentResolver getContentResolver() {
        return getContext().getContentResolver();
    }

    /**
     * Download enough extra subjects to meet our minimum number.
     *
     * @return Return true if we know for sure that no further downloading is currently necessary.
     */
    private boolean downloadMinimumSubjectsAsync() {
        final int missing = getNotDoneNeededForCache();
        if (missing > 0) {
            requestMoreItemsAsync(missing);
            return false;
        } else {
            return true; //Tell the caller that no action was necessary.
        }
    }

    private void requestMoreItemsAsync(int count) {
        if(mRequestMoreItemsAsyncInProgress) {
            //Do just one of these at a time,
            //to avoid requesting more while we are processing the results from a first one.
            //Then we get more than we really want and everything is slower.
            //TODO: This may be unnecessary with the SyncAdapter.
            return;
        }

        mRequestMoreItemsAsyncInProgress = true;

        final QueryAsyncTask task = new QueryAsyncTask();
        task.execute(count);
    }

    private int getNotDoneNeededForCache() {
        final int count = getNotDoneCount();
        final int min_cache_size = getMinCacheSize();
        return min_cache_size - count;
    }

    private int getNotDoneCount() {
        final ContentResolver resolver = getContentResolver();

        final String[] projection = {COUNT_AS_COUNT};
        final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                getWhereClauseForNotDone(), null, null);
        c.moveToFirst();
        final int result = c.getInt(0);
        c.close();
        return result;
    }

    private int getUploadedCount() {
        final ContentResolver resolver = getContentResolver();

        final String[] projection = {COUNT_AS_COUNT};
        final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                getWhereClauseForUploaded(), new String[]{}, null);

        c.moveToFirst();
        final int result = c.getInt(0);
        c.close();
        return result;
    }

    /**
     * Upload any outstanding classifications.
     *
     * @return Return true if we know for sure that no further uploading is currently necessary.
     */
    private boolean uploadOutstandingClassifications() {
        //To keep things simple, don't do this while it is already happening.
        //This only ever happens on this thread so there should be no need for a lock here.
        if (mUploadsInProgress > 0)
            return false;

        // TODO: Request re-authentication when the server says we have used the wrong name + api_key.
        // What does the server reply in that case?
        final LoginUtils.LoginDetails loginDetails = LoginUtils.getAccountLoginDetails(getContext());

        // query the database for any item whose classification is not yet uploaded.
        final ContentResolver resolver = getContentResolver();

        final String[] projection = {Item.Columns._ID,
                Item.Columns.SUBJECT_ID};
        final String whereClause =
                "(" + Item.Columns.DONE + " == 1) AND " +
                        "(" + Item.Columns.UPLOADED + " != 1)";
        final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                whereClause, new String[]{}, null); //TODO: Order by?

        if (c.getCount() == 0) {
            c.close();
            return true; //Tell the caller that no action was necessary.
        }

        while (c.moveToNext()) {
            final String itemId = c.getString(0);
            final String subjectId = c.getString(1);

            mUploadsInProgress++;
            final UploadAsyncTask task = new UploadAsyncTask();
            task.execute(itemId, subjectId, loginDetails.name, loginDetails.authApiKey);
        }

        c.close();
        return false;
    }

    /**
     * Remove old classified subjects if we have too many.
     *
     * @return Return true if we know for sure that no further removal is currently necessary.
     */
    private boolean removeOldSubjects() {
        final int count = getUploadedCount();
        final int max = getKeepCount();
        if (count > max) {
            Log.info("removeOldSubjects(): start");
            //Get the oldest done (and uploaded) items:
            final ContentResolver resolver = getContentResolver();

            final String[] projection = {Item.Columns._ID};
            //ISO-8601 dates can be alphabetically sorted to get date-time order:
            final String orderBy = Item.Columns.DATETIME_DONE + " ASC";
            final int countToRemove = count - max;
            //TODO: Use this: final String limit = Integer.toString(countToRemove); //TODO: Is this locale-independent?
            final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                    getWhereClauseForUploaded(), new String[]{}, orderBy);

            //Remove them one by one:
            int removed = 0;
            while (c.moveToNext()) {
                final String itemId = c.getString(0);
                if (!TextUtils.isEmpty(itemId)) {
                    removeItem(itemId);

                    //Only remove enough:
                    removed++;
                    if (removed == countToRemove) {
                        break;
                    }
                }
            }

            c.close();

            Log.info("removeOldSubjects(): end");

            return false;
        } else {
            return true; //Tell the caller that no action was necessary.
        }
    }

    private void removeItem(final String itemId) {
        final ContentResolver resolver = getContentResolver();

        //ItemsContentProvider takes care of deleting related files, classification answers, etc:
        if(resolver.delete(getItemUri(itemId), null, null) < 1) {
            Log.error("removeItem(): No item rows were removed.");
        }
    }

    private Uri getItemUri(final String itemId) {
        final Uri.Builder uriBuilder = Item.ITEMS_URI.buildUpon();
        uriBuilder.appendPath(itemId);
        return uriBuilder.build();
    }

    private class QueryAsyncTask extends AsyncTask<Integer, Integer, List<ZooniverseClient.Subject>> {
        @Override
        protected List<ZooniverseClient.Subject> doInBackground(final Integer... params) {
            if (params.length < 1) {
                Log.error("QueryAsyncTask: not enough params.");
                return null;
            }

            Log.info("QueryAsyncTask.doInBackground()");

            //TODO: Why not just set these in the constructor?
            //That seems to be allowed:
            //See Memory observability here:
            //http://developer.android.com/reference/android/os/AsyncTask.html
            final int count = params[0];

            try {
                return mClient.requestMoreItemsSync(count);
            } catch (final HttpUtils.NoNetworkException e) {
                Log.info("QueryAsyncTask.doInBackground(): No network connection.", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final List<ZooniverseClient.Subject> result) {
            super.onPostExecute(result);

            onQueryTaskFinished(result);
        }
    }

    private Boolean doUploadSync(final String itemId, final String subjectId, final String authName, final String authApiKey) {

        final String PARAM_PART_CLASSIFICATION = "classification";

        //Note: I tried using HttpPost.getParams().setParameter() instead of the NameValuePairs,
        //but that did not allow multiple parameters with the same name, which we need.
        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair(PARAM_PART_CLASSIFICATION + "[subject_ids][]",
                subjectId));

        final ContentResolver resolver = getContentResolver();

        //Mark it as a favorite if necessary:
        {
            final String[] projection = {Item.Columns.FAVORITE};
            final Cursor c = resolver.query(getItemUri(itemId),
                    projection, null, new String[]{}, null);

            if (c.moveToFirst()) {
                final int favorite = c.getInt(0);
                if (favorite == 1) {
                    nameValuePairs.add(new BasicNameValuePair(PARAM_PART_CLASSIFICATION + "[favorite][]",
                            "true"));
                }
            }

            c.close();
        }


        Cursor c;
        {
            final String selection = ClassificationAnswer.Columns.ITEM_ID + " = ?"; //We use ? to avoid SQL Injection.
            final String[] selectionArgs = {itemId};
            final String[] projection = {ClassificationAnswer.Columns.SEQUENCE,
                    ClassificationAnswer.Columns.QUESTION_ID,
                    ClassificationAnswer.Columns.ANSWER_ID};
            final String orderBy = ClassificationAnswer.Columns.SEQUENCE + " ASC";
            c = resolver.query(ClassificationAnswer.CONTENT_URI,
                    projection, selection, selectionArgs, orderBy);
        }

        while (c.moveToNext()) {
            final int sequence = c.getInt(0);
            final String questionId = c.getString(1);
            final String answerId = c.getString(2);

            //Add the question's answer:
            //TODO: Is the string representation of sequence locale-dependent?
            final String questionKey =
                    PARAM_PART_CLASSIFICATION + "[annotations][" + sequence + "][" + questionId + "]";
            nameValuePairs.add(new BasicNameValuePair(questionKey, answerId));


            final String selection = "(" + ClassificationCheckbox.Columns.ITEM_ID + " = ?) AND " +
                    "(" + ClassificationCheckbox.Columns.QUESTION_ID + " == ?)"; //We use ? to avoid SQL Injection.
            final String[] selectionArgs = {itemId, questionId};

            //Add the question's answer's selected checkboxes, if any:
            //The sequence will be the same for any selected checkbox for the same answer,
            //so we don't bother getting that, or sorting by that.
            final String[] projection = {ClassificationCheckbox.Columns.CHECKBOX_ID};
            final String orderBy = ClassificationCheckbox.Columns.CHECKBOX_ID + " ASC";
            final Cursor cursorCheckboxes = resolver.query(ClassificationCheckbox.CONTENT_URI,
                    projection, selection, selectionArgs, orderBy);

            while (cursorCheckboxes.moveToNext()) {
                final String checkboxId = cursorCheckboxes.getString(0);

                //TODO: The Galaxy-Zoo server expects us to reuse the parameter name,
                //TODO: Is the string representation of sequence locale-dependent?
                nameValuePairs.add(new BasicNameValuePair(questionKey, checkboxId));
            }

            cursorCheckboxes.close();
        }

        c.close();


        return mClient.uploadClassificationSync(authName, authApiKey, nameValuePairs);
    }

    private class UploadAsyncTask extends AsyncTask<String, Integer, Boolean> {

        private String mItemId = null;

        @Override
        protected Boolean doInBackground(final String... params) {
            if (params.length < 4) {
                Log.error("UploadAsyncTask: not enough params.");
                return false;
            }

            Log.info("UploadAsyncTask.doInBackground()");

            //TODO: Why not just set these in the constructor?
            //That seems to be allowed:
            //See Memory observability here:
            //http://developer.android.com/reference/android/os/AsyncTask.html
            mItemId = params[0];
            final String subjectId = params[1];

            final String authName = params[2];
            final String authApiKey = params[3];


            return doUploadSync(mItemId, subjectId, authName, authApiKey);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            onUploadTaskFinished(result, mItemId);
        }
    }

    private void onQueryTaskFinished(final List<ZooniverseClient.Subject> result) {
        mRequestMoreItemsAsyncInProgress = false;

        if (result == null) {
            return;
        }

        //Check that we are not adding too many,
        //which can happen if a second request was queued before we got the result from a
        //first request.
        List<ZooniverseClient.Subject> listToUse = result;
        final int missing = getNotDoneNeededForCache();
        if (missing <= 0) {
            return;
        }

        final int size = result.size();
        if (missing < size) {
            listToUse = result.subList(0, missing);
        }
        addSubjects(listToUse, true /* async */);
    }

    /**
     * @param subjects
     * @param asyncFileDownloads Get the image data asynchronously if this is true.
     */
    private void addSubjects(final List<ZooniverseClient.Subject> subjects, boolean asyncFileDownloads) {
        if (subjects == null) {
            return;
        }

        for (final ZooniverseClient.Subject subject : subjects) {
            addSubject(subject, asyncFileDownloads);
        }
    }

    /*
    private File getFile(long id) {
        return new File(getContext().getExternalFilesDir(null), Long
                .toString(id)
                + ".glom");
    }
    */

    private void onUploadTaskFinished(boolean result, final String itemId) {
        if (result) {
            markItemAsUploaded(itemId);
        } else {
            //TODO: Inform the user?
        }

        mUploadsInProgress--;
    }

    private void markItemAsUploaded(final String itemId) {
        final ContentValues values = new ContentValues();
        values.put(Item.Columns.UPLOADED, 1);

        final ContentResolver resolver = getContentResolver();
        final int affected = resolver.update(getItemUri(itemId),
                values, null, null);

        if (affected != 1) {
            Log.error("markItemAsUploaded(): Unexpected affected rows: " + affected);
        }
    }

    /**
     * Download bytes from a url and store them in a file, optionally asynchronously in spawned thread.
     *
     * @param asyncFileDownloads Get the image data asynchronously if this is true.
     */
    private boolean cacheUriToFile(final String uriFileToCache, final String cacheFileUri, final Uri itemUri, final ImageType imageType, boolean asyncFileDownloads) {
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
            task.execute(uriFileToCache, cacheFileUri);
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

    private void markImageDownloadAsNotInProgress(final String uriFileToCache) {
        mImageDownloadsInProgress.remove(uriFileToCache);
    }

    private boolean markImageAsDownloaded(final Uri itemUri, final ImageType imageType, final String uriFileToCache) {

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

        final ContentResolver resolver = getContentResolver();

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
    private void addSubject(final ZooniverseClient.Subject item, boolean asyncFileDownloads) {
        if (subjectIsInDatabase(item.mSubjectId)) {
            //It is already in the database.
            //TODO: Update the row?
            return;
        }

        final ContentResolver resolver = getContentResolver();

        final ContentValues values = new ContentValues();
        values.put(Item.Columns.SUBJECT_ID, item.mSubjectId);
        values.put(Item.Columns.ZOONIVERSE_ID, item.mZooniverseId);

        //The ItemsContentProvider will take care of creating local file URIs for the remote URis,
        //and this SyncAdapter will request that the remote image files are downloaded into those local file URIs.
        values.put(Item.Columns.LOCATION_STANDARD_URI_REMOTE, item.mLocationStandard);
        values.put(Item.Columns.LOCATION_THUMBNAIL_URI_REMOTE, item.mLocationThumbnail);
        values.put(Item.Columns.LOCATION_INVERTED_URI_REMOTE, item.mLocationInverted);

        final Uri itemUri = resolver.insert(Item.ITEMS_URI, values);
        if (itemUri == null) {
            throw new IllegalStateException("could not insert " +
                    "content values: " + values);
        }

        cacheUrisToFiles(itemUri, asyncFileDownloads);

        //TODO: notifyRowChangeById(rowId);
    }

    private void cacheUrisToFiles(final Uri itemUri, boolean asyncFileDownloads) {

        final ContentResolver resolver = getContentResolver();

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

    public static class FileCacheAsyncTask extends AsyncTask<String, Integer, Boolean> {

        private final Uri itemUri;
        private final ImageType imageType;
        private String uriFileToCache = null;
        private final WeakReference<SyncAdapter> syncAdapterReference;

        public FileCacheAsyncTask(final SyncAdapter syncAdapter, final Uri itemUri, ImageType imageType) {
            this.itemUri = itemUri;
            this.imageType = imageType;
            this.syncAdapterReference = new WeakReference<>(syncAdapter);
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

            final SyncAdapter syncAdapter = getSyncAdapter();
            if(syncAdapter == null) {
                Log.error("FileCacheAsyncTask(): donBackground(): syncAdapter is null.");
                return false;
            }

            return HttpUtils.cacheUriToContentUriFileSync(syncAdapter.getContext(), uriFileToCache, cacheFileUri);
        }

        private SyncAdapter getSyncAdapter() {
            if (syncAdapterReference == null) {
                return null;
            }

            return syncAdapterReference.get();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            final SyncAdapter syncAdapter = getSyncAdapter();
            if (syncAdapter == null) {
                Log.error("FileCacheAsyncTask.onPostExecute(): SyncAdapter is null.");
                return;
            }

            syncAdapter.markImageDownloadAsNotInProgress(uriFileToCache);

            if (result) {
                if (!syncAdapter.markImageAsDownloaded(itemUri, imageType, uriFileToCache)) {
                    Log.error("FileCacheAsyncTask(): onPostExecute(): markImageAsDownloaded() failed.");
                }
            } else {
                //doRegularTasks() will try again later.
                Log.error("FileCacheAsyncTask(): cacheUriToContentUriFileSync(): failed.");
            }

            super.onPostExecute(result);
        }
    }


    private String getWhereClauseForNotDone() {
        return Item.Columns.DONE + " != 1";
    }

    private String getWhereClauseForUploaded() {
        return Item.Columns.UPLOADED + " == 1";
    }

    private String getWhereClauseForDownloadNotDone() {
        return "(" +
                Item.Columns.LOCATION_STANDARD_DOWNLOADED + " != 1" +
                ") OR (" +
                Item.Columns.LOCATION_THUMBNAIL_DOWNLOADED + " != 1" +
                ") OR (" +
                Item.Columns.LOCATION_INVERTED_DOWNLOADED + " != 1" +
                ")";
    }

    private int getMinCacheSize() {
        return Utils.getIntPref(getContext(), R.string.pref_key_cache_size);
    }

    private int getKeepCount() {
        return Utils.getIntPref(getContext(), R.string.pref_key_keep_count);
    }


    private boolean subjectIsInDatabase(final String subjectId) {
        //TODO: Use COUNT_AS_COUNT ?
        final ContentResolver resolver = getContentResolver();

        final String[] projection = {Item.Columns.SUBJECT_ID};
        final String whereClause = Item.Columns.SUBJECT_ID + " = ?"; //We use ? to avoid SQL Injection.
        final String[] selectionArgs = {subjectId};
        final Cursor c = resolver.query(Item.ITEMS_URI, projection,
                whereClause, selectionArgs, null);
        final boolean result = c.getCount() > 0;
        c.close();
        return result;
    }

    private void throwIfNoNetwork() {
        HttpUtils.throwIfNoNetwork(getContext());
    }
}
