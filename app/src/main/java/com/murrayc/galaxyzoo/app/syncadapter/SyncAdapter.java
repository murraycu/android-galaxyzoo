package com.murrayc.galaxyzoo.app.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.R;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.ClassificationAnswer;
import com.murrayc.galaxyzoo.app.provider.ClassificationCheckbox;
import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;
import com.murrayc.galaxyzoo.app.provider.Item;
import com.murrayc.galaxyzoo.app.provider.client.ZooniverseClient;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 10/4/14.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String COUNT_AS_COUNT = "COUNT(*) AS count";
    private int mUploadsInProgress = 0;

    private boolean mRequestMoreItemsAsyncInProgress = false;

    //This communicates with the remote server:

    private ZooniverseClient mClient = null;

    //This does some of the work to communicate with the itemsContentProvider
    //and download image files to the local cache.
    private SubjectAdder mSubjectAdder = null;

    public SyncAdapter(final Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mClient = new ZooniverseClient(context, Config.SERVER);
        mSubjectAdder = new SubjectAdder(context);

        try {
            PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
        } catch (final UnsupportedOperationException e) {
            //This happens during our test case, because the MockContext doesn't support this,
            //so ignore this.
        }
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

        //Get all the items that have an image that is not yet fully downloaded:

        //Find out if the image is currently being downloaded:

        return mSubjectAdder.downloadMissingImages();
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
        if(resolver.delete(Utils.getItemUri(itemId), null, null) < 1) {
            Log.error("removeItem(): No item rows were removed.");
        }
    }

    private class QueryAsyncTask extends AsyncTask<Integer, Integer, List<ZooniverseClient.Subject>> {
        @Override
        protected List<ZooniverseClient.Subject> doInBackground(final Integer... params) {
            if (params.length < 1) {
                Log.error("QueryAsyncTask: not enough params.");
                return null;
            }

            //TODO: Why not just set these in the constructor?
            //That seems to be allowed:
            //See Memory observability here:
            //http://developer.android.com/reference/android/os/AsyncTask.html
            final int count = params[0];

            Log.info("QueryAsyncTask.doInBackground(): count=" + count);

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
            final Cursor c = resolver.query(Utils.getItemUri(itemId),
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
        mSubjectAdder.addSubjects(listToUse, true /* async */);
    }

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
        final int affected = resolver.update(Utils.getItemUri(itemId),
                values, null, null);

        if (affected != 1) {
            Log.error("markItemAsUploaded(): Unexpected affected rows: " + affected);
        }
    }

    private static String getWhereClauseForNotDone() {
        return Item.Columns.DONE + " != 1";
    }

    private static String getWhereClauseForUploaded() {
        return Item.Columns.UPLOADED + " == 1";
    }

    private int getMinCacheSize() {
        return Utils.getIntPref(getContext(), R.string.pref_key_cache_size);
    }

    private int getKeepCount() {
        return Utils.getIntPref(getContext(), R.string.pref_key_keep_count);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        final Context context = getContext();

        //Changes to these preferences would need us to do some work:
        if (TextUtils.equals(key, context.getString(R.string.pref_key_cache_size)) ||
                TextUtils.equals(key, context.getString(R.string.pref_key_keep_count))) {
            doRegularTasks();
        }
    }

}
