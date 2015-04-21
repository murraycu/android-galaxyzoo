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

package com.murrayc.galaxyzoo.app.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.R;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.ClassificationAnswer;
import com.murrayc.galaxyzoo.app.provider.ClassificationCheckbox;
import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;
import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.client.MoreItemsJsonParser;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 10/4/14.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String COUNT_AS_COUNT = "COUNT(*) AS count";
    private static final String PARAM_PART_CLASSIFICATION = "classification";
    private static final String WHERE_CLAUSE_NOT_DONE = Item.Columns.DONE + " != 1";
    private static final String WHERE_CLAUSE_UPLOADED = Item.Columns.UPLOADED + " == 1";
    public static final String[] PROJECTION_ITEMS_OUTSTANDING = {Item.Columns._ID,
            Item.Columns.SUBJECT_ID,
            Item.Columns.GROUP_ID};
    public static final String[] PROJECTION_ID = {Item.Columns._ID};
    public static final String[] PROJECTION_FAVORITE = {Item.Columns.FAVORITE};
    public static final String[] PROJECTION_CLASSIFICATION_CHECKBOX_ID = {ClassificationCheckbox.Columns.CHECKBOX_ID};
    private int mUploadsInProgress = 0;

    private boolean mRequestMoreItemsTaskInProgress = false;

    //This communicates with the remote server:

    private ZooniverseClient mClient = null;

    //This does some of the work to communicate with the itemsContentProvider
    //and download image files to the local cache.
    private final SubjectAdder mSubjectAdder;

    //Out Runnable tasks use this to post results back to our main thread.
    private final Handler mHandler;
    private static final String[] PROJECTION_UPLOAD = {ClassificationAnswer.Columns.SEQUENCE,
            ClassificationAnswer.Columns.QUESTION_ID,
            ClassificationAnswer.Columns.ANSWER_ID};
    private static final String[] PROJECTION_COUNT_AS_COUNT = new String[]{COUNT_AS_COUNT};

    public SyncAdapter(final Context context, final boolean autoInitialize) {
        super(context, autoInitialize);
        mHandler = new Handler(Looper.getMainLooper());

        mClient = new ZooniverseClient(context, Config.SERVER);
        mSubjectAdder = new SubjectAdder(context, mClient.getRequestQueue());

        //We don't listen for the SharedPreferences changes here because it doesn't currently
        //work across processes, so our listener would never be called.
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider, final SyncResult syncResult) {
        doRegularTasks();
    }

    /**
     * Do any uploads, downloads, or removals that are currently necessary.
     * This might not finish all necessary work, so subsequent calls might be necessary.
     *
     * @return Return true if we know for sure that no further work is currently necessary.
     */
    private void doRegularTasks() {
        Log.info("doRegularTasks() start");
        //Do the download first, to avoid the UI having to wait for new subjects to classify.


        downloadMinimumSubjectsAsync();
        downloadMissingImages();

        //Do less urgent things next:
        uploadOutstandingClassifications();
        removeOldSubjects();

        //TODO: Don't bother checking that each image still exists, repeatedly -
        //instead only check if a special file has been removed from the cache?
        checkImagesStillExist();

        Log.info("doRegularTasks() end");
    }

    private boolean checkImagesStillExist() {
        Log.info("checkImagesStillExist(): start");
        final boolean noWorkNecessary = mSubjectAdder.checkForDeletedCachedImages();
        Log.info("checkImagesStillExist(): end");

        return noWorkNecessary;
    }

    /**
     * Download any images that have previously failed to download.
     *
     * @return Return true if we know for sure that no further downloading is currently necessary.
     */
    private boolean downloadMissingImages() {

        //Get all the items that have an image that is not yet fully downloaded:

        //Find out if the image is currently being downloaded:

        try {
            return mSubjectAdder.downloadMissingImages();
        } catch (final HttpUtils.NoNetworkException e) {
            //Ignore this - it is normal if wifi-only is set in the settings
            //and if we are then not on a wi-fi connection.
            Log.info("SyncAdapter.downloadMissingImages(): Ignoring NoNetworkException.");
            return false;
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

    private void requestMoreItemsAsync(final int count) {
        if(mRequestMoreItemsTaskInProgress) {
            //Do just one of these at a time,
            //to avoid requesting more while we are processing the results from a first one.
            //Then we get more than we really want and everything is slower.
            //TODO: This may be unnecessary with the SyncAdapter.
            return;
        }



        mRequestMoreItemsTaskInProgress = true;

        try {
            mClient.requestMoreItemsAsync(count,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(final String response) {
                            final List<ZooniverseClient.Subject> result = MoreItemsJsonParser.parseMoreItemsResponseContent(response);
                            onQueryTaskFinished(result);
                            mRequestMoreItemsTaskInProgress = false;
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(final VolleyError error) {
                            Log.error("ZooniverseClient.requestMoreItemsSync(): request failed", error);
                            mRequestMoreItemsTaskInProgress = false;
                        }
                    });
        } catch (final HttpUtils.NoNetworkException e) {
            //Ignore this - it is normal if wifi-only is set in the settings
            //and if we are then not on a wi-fi connection.
            Log.info("SyncAdapter.requestMoreItemsAsync(): Ignoring NoNetworkException.");
            mRequestMoreItemsTaskInProgress = false;
        }

    }

    private int getNotDoneNeededForCache() {
        final int count = getNotDoneCount();
        final int min_cache_size = getMinCacheSize();
        return min_cache_size - count;
    }

    private int getNotDoneCount() {
        final ContentResolver resolver = getContentResolver();

        final Cursor c = resolver.query(Item.ITEMS_URI, PROJECTION_COUNT_AS_COUNT,
                WHERE_CLAUSE_NOT_DONE, null, null);
        c.moveToFirst();
        final int result = c.getInt(0);
        c.close();
        return result;
    }

    private int getUploadedCount() {
        final ContentResolver resolver = getContentResolver();

        final Cursor c = resolver.query(Item.ITEMS_URI, PROJECTION_COUNT_AS_COUNT,
                WHERE_CLAUSE_UPLOADED, new String[]{}, null);

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
        // See https://github.com/zooniverse/Galaxy-Zoo/issues/184
        final LoginUtils.LoginDetails loginDetails = LoginUtils.getAccountLoginDetails(getContext());

        // query the database for any item whose classification is not yet uploaded.
        final ContentResolver resolver = getContentResolver();

        final String whereClause =
                "(" + Item.Columns.DONE + " == 1) AND " +
                        "(" + Item.Columns.UPLOADED + " != 1)";
        final Cursor c = resolver.query(Item.ITEMS_URI, PROJECTION_ITEMS_OUTSTANDING,
                whereClause, new String[]{}, null); //TODO: Order by?

        if (c.getCount() == 0) {
            c.close();
            return true; //Tell the caller that no action was necessary.
        }

        while (c.moveToNext()) {
            final String itemId = c.getString(0);
            final String subjectId = c.getString(1);
            final String groupId = c.getString(2);


            mUploadsInProgress++;
            final UploadTask task = new UploadTask(itemId, subjectId, groupId, loginDetails.name, loginDetails.authApiKey);
            final Thread thread = new Thread(task);
            thread.start();
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

            //ISO-8601 dates can be alphabetically sorted to get date-time order:
            final String orderBy = Item.Columns.DATETIME_DONE + " ASC";
            final int countToRemove = count - max;
            //TODO: Use this: final String limit = Integer.toString(countToRemove); //TODO: Is this locale-independent?
            final Cursor c = resolver.query(Item.ITEMS_URI, PROJECTION_ID,
                    WHERE_CLAUSE_UPLOADED, new String[]{}, orderBy);

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
        if(resolver.delete(Utils.getItemUri(itemId), null, null) < 1) {
            Log.error("removeItem(): No item rows were removed.");
        }
    }

    private boolean doUploadSync(final String itemId, final String subjectId, final String groupId, final String authName, final String authApiKey) throws ZooniverseClient.UploadException {

        //Note: I tried using HttpPost.getParams().setParameter() instead of the NameValuePairs,
        //but that did not allow multiple parameters with the same name, which we need.
        final List<ZooniverseClient.NameValuePair> nameValuePairs = new ArrayList<>();

        nameValuePairs.add(new ZooniverseClient.NameValuePair(PARAM_PART_CLASSIFICATION + "[subject_ids][]",
                subjectId));

        final ContentResolver resolver = getContentResolver();

        //Mark it as a favorite if necessary:
        {
            final Cursor c = resolver.query(Utils.getItemUri(itemId),
                    PROJECTION_FAVORITE, null, new String[]{}, null);

            if (c.moveToFirst()) {
                final int favorite = c.getInt(0);
                if (favorite == 1) {
                    nameValuePairs.add(new ZooniverseClient.NameValuePair(PARAM_PART_CLASSIFICATION + "[favorite][]",
                            "true"));
                }
            }

            c.close();
        }


        final Cursor c;
        {
            final String selection = ClassificationAnswer.Columns.ITEM_ID + " = ?"; //We use ? to avoid SQL Injection.
            final String[] selectionArgs = {itemId};
            final String orderBy = ClassificationAnswer.Columns.SEQUENCE + " ASC";
            c = resolver.query(ClassificationAnswer.CONTENT_URI,
                    PROJECTION_UPLOAD, selection, selectionArgs, orderBy);
        }

        int max_sequence = 0;
        while (c.moveToNext()) {
            final int sequence = c.getInt(0);
            final String questionId = c.getString(1);
            final String answerId = c.getString(2);

            //We could instead ORDER BY the sequence but that might be slightly slower and need a index.
            if(sequence > max_sequence) {
                max_sequence = sequence;
            }

            //Add the question's answer:
            //TODO: Is the string representation of sequence locale-dependent?
            final String questionKey =
                    getAnnotationPart(sequence) + "[" + questionId + "]";
            nameValuePairs.add(new ZooniverseClient.NameValuePair(questionKey, answerId));


            final String selection = "(" + ClassificationCheckbox.Columns.ITEM_ID + " = ?) AND " +
                    "(" + ClassificationCheckbox.Columns.QUESTION_ID + " == ?)"; //We use ? to avoid SQL Injection.
            final String[] selectionArgs = {itemId, questionId};

            //Add the question's answer's selected checkboxes, if any:
            //The sequence will be the same for any selected checkbox for the same answer,
            //so we don't bother getting that, or sorting by that.
            final String orderBy = ClassificationCheckbox.Columns.CHECKBOX_ID + " ASC";
            final Cursor cursorCheckboxes = resolver.query(ClassificationCheckbox.CONTENT_URI,
                    PROJECTION_CLASSIFICATION_CHECKBOX_ID, selection, selectionArgs, orderBy);

            while (cursorCheckboxes.moveToNext()) {
                final String checkboxId = cursorCheckboxes.getString(0);

                //TODO: The Galaxy-Zoo server expects us to reuse the parameter name,
                //TODO: Is the string representation of sequence locale-dependent?
                nameValuePairs.add(new ZooniverseClient.NameValuePair(questionKey, checkboxId));
            }

            cursorCheckboxes.close();
        }

        c.close();


        //Help the server know that the classification is from this Android app,
        //by reusing the User-Agent string as a parameter value.
        //See https://github.com/murraycu/android-galaxyzoo/issues/11
        final String key =
                getAnnotationPart(max_sequence + 1 ) + "[user_agent]";
        nameValuePairs.add(new ZooniverseClient.NameValuePair(key, HttpUtils.USER_AGENT_MURRAYC));

        return mClient.uploadClassificationSync(authName, authApiKey, groupId, nameValuePairs);
    }

    private static String getAnnotationPart(final int sequence) {
        return PARAM_PART_CLASSIFICATION + "[annotations][" + sequence + "]";
    }

    private class UploadTask implements Runnable {
        private final String mItemId ;
        private final String mSubjectId;
        private final String mGroupId;
        private final String mAuthName;
        private final String mAuthApiKey;

        public UploadTask(final String itemId, final String subjectId, final String groupId, final String authName, final String authApiKey) {
            mItemId = itemId;
            mSubjectId = subjectId;
            mGroupId = groupId;
            mAuthName = authName;
            mAuthApiKey = authApiKey;
        }

        @Override
        public void run() {
            Log.info("UploadTask.run()");
            boolean result = false;
            try {
                result = doUploadSync(mItemId, mSubjectId, mGroupId, mAuthName, mAuthApiKey);
            } catch (final HttpUtils.NoNetworkException e) {
                //This is normal, if there is no suitable network connection.
                Log.info("UploadTask(): NoNetworkException");
            } catch (final ZooniverseClient.UploadException e) {
                Log.error("UploadTask(): UploadException", e);
            }

            //Call onPostExecute in the main thread:
            final boolean resultToUse = result;
            mHandler.post(new Runnable() {
                public void run() {
                    onPostExecute(resultToUse);
                }
            });
        }

        protected void onPostExecute(final boolean result) {
            onUploadTaskFinished(result, mItemId);
        }
    }

    private void onQueryTaskFinished(final List<ZooniverseClient.Subject> result) {
        mRequestMoreItemsTaskInProgress = false;

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
        mSubjectAdder.addSubjects(listToUse, true /* async */);
    }

    private void onUploadTaskFinished(final boolean result, final String itemId) {
        if (result) {
            markItemAsUploaded(itemId);
        } //else {
            //TODO: Inform the user?
        //}

        mUploadsInProgress--;
    }

    private void markItemAsUploaded(final String itemId) {
        final ContentValues values = new ContentValues();
        values.put(Item.Columns.UPLOADED, 1);

        final ContentResolver resolver = getContentResolver();
        final int affected = resolver.update(Utils.getItemUri(itemId),
                values, null, null);

        if (affected != 1) {
            Log.error("markItemAsUploaded(): Unexpected affected rows: " + affected);
        }
    }

    private int getMinCacheSize() {
        return LoginUtils.getIntPref(getContext(), R.string.pref_key_cache_size);
    }

    private int getKeepCount() {
        return LoginUtils.getIntPref(getContext(), R.string.pref_key_keep_count);
    }


}
