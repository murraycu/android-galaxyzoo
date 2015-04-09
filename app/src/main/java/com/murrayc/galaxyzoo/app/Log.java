/*
 * Copyright (C) 2011 Openismus GmbH
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


/**
 * A class that wraps methods in Log to add the calling method name from the servlet to
 * log messages.
 */
public final class Log {

    private static final String LOG_TAG = "android-galaxyzoo"; //TODO: Get this from the build files somehow.

    /* A replacement for StringUtils.defaultString(),
                 * because Android's TextUtils doesn't have it.
                 */
    private static String defaultString(final String str) {
        if (str == null)
            return "";

        return str;
    }

    // Fatal methods
    public static void fatal(final String message, final Throwable e) {
        fatal(defaultString(message) + ":" + e.getMessage());
    }

    private static void fatal(final String message) {
        android.util.Log.e(LOG_TAG, defaultString(message));
    }

    // Error methods
    public static void error(final String message, final Throwable e) {
        android.util.Log.e(LOG_TAG, defaultString(message), e);
    }

    public static void error(final String message) {
        android.util.Log.e(LOG_TAG, defaultString(message));
    }

    // Warning methods
    private static void warn(final String message, final Throwable e) {
        warn(defaultString(message) + ": " + e.getMessage());
    }

    private static void warn(final String message) {
        android.util.Log.w(LOG_TAG, defaultString(message));
    }

    // Info methods
    public static void info(final String message, final Throwable e) {
        android.util.Log.i(LOG_TAG, defaultString(message) + ": " + e.getMessage());
    }

    public static void info(final String message) {
        android.util.Log.i(LOG_TAG, defaultString(message));
    }
}
