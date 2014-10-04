package com.murrayc.galaxyzoo.app.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

/**
 * Created by murrayc on 10/4/14.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    public SyncAdapter(final Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider, SyncResult syncResult) {

    }
}
