/*
 * Copyright (C) 2015 Murray Cumming
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

package com.murrayc.galaxyzoo.app;

import android.app.Application;
import android.net.Uri;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;

/**
 * Created by murrayc on 5/12/15.
 */
public class GalaxyZooApplication extends Application {

    static final Picasso.Listener picassoListener = new PicassoListener();

    @Override
    public void onCreate() {
        super.onCreate();

        //Catch leaks, in debug builds (release builds use a no-op).
        LeakCanary.install(this);

        //Let us log errors from Picasso to give us some clues when things go wrong.
        //Unfortunately, we can't get these errors in the regular onError() callback:
        //https://github.com/square/picasso/issues/379
        final Picasso picasso = (new Picasso.Builder(this)).listener(GalaxyZooApplication.picassoListener).build();
        //This affects what, for instance, Picasso.with() will return:
        try {
            Picasso.setSingletonInstance(picasso);
        } catch (final IllegalStateException ex) {
            //Nevermind if this happens. It's not worth crashing the app because of this.
            //It would just mean that we don't log the errors.
            Log.error("GalaxyZooApplication.onCreate(): It is too late to call Picasso.setSingletonInstance().", ex);
        }
    }

    private static class PicassoListener implements Picasso.Listener {
        @Override
        public void onImageLoadFailed(final Picasso picasso, final Uri uri, final Exception exception) {
            Log.error("Picasso onImageLoadFailed() URI=" + uri, exception);
        }
    }
}