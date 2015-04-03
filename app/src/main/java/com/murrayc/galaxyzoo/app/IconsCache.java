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

package com.murrayc.galaxyzoo.app;

import android.content.Context;
//import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.Volley;
//import com.murrayc.galaxyzoo.app.provider.HttpUtils;
//import com.murrayc.galaxyzoo.app.syncadapter.SubjectAdder;

//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;

class IconsCache {
    //TODO: Generate these automatically, making sure they are unique:
    /*
    private static final String CACHE_FILE_WORKFLOW_ICONS = "workflowicons";
    private static final String CACHE_FILE_EXAMPLE_ICONS = "exampleicons";
    private static final String CACHE_FILE_CSS = "css";
    */

    private static final String ASSET_PATH_ICONS_DIR = "icons/";
    private static final String ICON_FILE_PREFIX = "icon_";
    //private static long ASSETS_ICONS_TIMESTAMP = 1409922463000L; //Update this when bundling new copies of the files.

    //private final File mCacheDir;

    //TODO: Don't put both kinds of icons in the same map:
    //See this about the use of the LruCache:
    //http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html#memory-cache
    private final LruCache<String, Bitmap> mWorkflowIcons = new LruCache<>(20);
    private final LruCache<String, Bitmap> mExampleIcons = new LruCache<>(20);
    private final Context mContext;
    /*
    private Bitmap mBmapWorkflowIcons = null;
    private Bitmap mBmapExampleIcons = null;
    private RequestQueue mRequestQueue = null;
    */

    /**
     * This does network IO so it should not be used in the UI's main thread.
     * For instance, do this in an AsyncTask, as in Singleton.init().
     *
     * @param context
     * @param decisionTrees Decision trees whose icons should be pre-loaded.
     */
    public IconsCache(final Context context, final Map<String, DecisionTree> decisionTrees) {
        this.mContext = context;
        /* this.mRequestQueue = Volley.newRequestQueue(context);

        mCacheDir = Utils.getExternalCacheDir(context);
        if (mCacheDir == null) {
            //This would probably lead to a crash later:
            Log.error("IconsCache(): getExternalCacheDir() returned null.");
        }

        long lastModified = 0;

        boolean loadFromNetwork = true;

        if (loadFromNetwork) {
            loadFromNetwork(context, lastModified);
        } else {

         */

        //Just get the cached icons:
        if (!reloadCachedIcons(decisionTrees)) {
            //Something went wrong while reloading the icons from the cache files,
            Log.error("IconsCache: reloadCachedIcons() failed.");

            /*
            //So try loading them again.
            if ((networkConnected != null) || (networkConnected.connected)) {
                Log.info("IconsCache(): Reloading the icons from the network after failing to reload them from the cache.");
                loadFromNetwork(context, lastModified);
            }
            */
        }

        /* }

        mBmapWorkflowIcons = null;
        mBmapExampleIcons = null;
        */
    }

    /*
    private void loadFromNetwork(final Context context, long lastModified) {
        //Get the updated files from the server and re-process them:
        readIconsFileSync(Config.ICONS_URI, CACHE_FILE_WORKFLOW_ICONS);
        readIconsFileSync(Config.EXAMPLES_URI, CACHE_FILE_EXAMPLE_ICONS);
        readCssFileSync(com.murrayc.galaxyzoo.app.Config.ICONS_CSS_URI, CACHE_FILE_CSS);
    }

    private static String getPrefKeyIconCacheLastMod(Context context) {
        return context.getString(R.string.pref_key_icons_cache_last_mod);
    }
    */

    /**
     *
     * @param decisionTrees Decision Trees whose icons should be pre-loaded.
     * @return
     */
    private boolean reloadCachedIcons(final Map<String, DecisionTree> decisionTrees) {
        mWorkflowIcons.evictAll();
        mExampleIcons.evictAll();

        boolean allSucceeded = true;

        //For each tree, try loading all its icons:
        for (final DecisionTree decisionTree : decisionTrees.values()) {
            final List<DecisionTree.Question> questions = decisionTree.getAllQuestions();
            for (final DecisionTree.Question question : questions) {
                if (!reloadIconsForQuestion(question)) {
                    allSucceeded = false;
                    //But keep on trying the other ones.
                }
            }
        }

        return allSucceeded;
    }

    private boolean reloadIconsForQuestion(final DecisionTree.Question question) {
        for (final DecisionTree.Answer answer : question.getAnswers()) {
            //Get the icon for the answer:
            if (!reloadIcon(answer.getIcon(), mWorkflowIcons)) {
                return false;
            }

            if (!reloadExampleImages(question, answer)) {
                return false;
            }
        }

        for (final DecisionTree.Checkbox checkbox : question.getCheckboxes()) {
            if (!reloadIcon(checkbox.getIcon(), mWorkflowIcons)) {
                return false;
            }

            if (!reloadExampleImages(question, checkbox)) {
                return false;
            }
        }

        return true;
    }

    private boolean reloadExampleImages(final DecisionTree.Question question, final DecisionTree.BaseButton answer) {
        //Get the example images for the answer or checkbox:
        for (int i = 0; i < answer.getExamplesCount(); i++) {
            final String exampleIconName = answer.getExampleIconName(question.getId(), i);
            if (!reloadIcon(exampleIconName, mExampleIcons)) {
                return false;
            }
        }

        return true;
    }

    private boolean reloadIcon(final String cssName, final LruCache<String, Bitmap> map) {
        //LruCache throws exceptions on null keys or values.
        if (TextUtils.isEmpty(cssName)) {
            return false;
        }

        //Log.info("reloadIcon:" + cssName);

        //Avoid loading and adding it again:
        if (map.get(cssName) != null) {
            return true;
        }

        /*
        Bitmap bitmap = null;

        //First get it from the cache, because that would be newer than the bundled asset:
        final String cacheFileUri = getCacheIconFileUri(cssName);
        if (TextUtils.isEmpty(cacheFileUri)) {
            return false;
        }

        final File cacheFile = new File(cacheFileUri);
        if (cacheFile.exists()) {
            bitmap = BitmapFactory.decodeFile(cacheFileUri);
            if (bitmap == null) {
                //The file contents are invalid.
                //Maybe the download was incomplete or something else odd happened.
                //Anyway, we should stop trying to use it,
                //And tell the caller about the failure,
                //so we can reload it by reloading and reparsing everything.
                Log.error("IconsCache.reloadIcon(): BitmapFactory.decodeFile() failed for file (now deleting it): ", cacheFileUri);

                final File file = new File(cacheFileUri);
                if (!file.delete()) {
                    Log.error("IconsCache.reloadIcon(): Failed to delete invalid cache file.");
                    return false;
                }
            }
        }

        if (bitmap == null) {
        */

        //We bundle the icons with the app,
        //so fall back to that:
        Bitmap bitmap = null;
        final InputStream inputStreamAsset = Utils.openAsset(getContext(), getIconAssetPath(cssName));
        if(inputStreamAsset != null) {
            bitmap = BitmapFactory.decodeStream(inputStreamAsset);
        }
        //}

        if (bitmap == null) {
            Log.error("reloadIcon(): Could not load icon: " + cssName);
            return false;
        }

        map.put(cssName, bitmap);
        return true;
    }

    private static String getIconAssetPath(final String cssName) {
        return ASSET_PATH_ICONS_DIR + ICON_FILE_PREFIX + cssName;
    }

    /*
    private void readIconsFileSync(final String uriStr, final String cacheId) {
        final String cacheFileUri = createCacheFile(cacheId);
        try {
            if(!HttpUtils.cacheUriToFileSync(getContext(), mRequestQueue, uriStr, cacheFileUri)) {
                Log.error("readIconsFileSync(): cacheUriToFileSync() failed.");
            }
        } catch (final HttpUtils.FileCacheException e) {
            Log.error("readIconsFileSync: Exception from HttpUtils.cacheUriToFileSync", e);
        }
    }

    private void readCssFileSync(final String uriStr, final String cacheId) {
        final String cacheFileUri = createCacheFile(cacheId);
        try {
            if(!HttpUtils.cacheUriToFileSync(getContext(), mRequestQueue, uriStr, cacheFileUri)) {
                Log.error("readCssFileSync(): cacheUriToFileSync() failed.");
                //TODO: Try again?
            } else {
                onCssDownloaded();
            }
        } catch (final HttpUtils.FileCacheException e) {
            Log.error("readIconsFileSync: Exception from HttpUtils.cacheUriToFileSync", e);
        }
    }

    private void cacheIconsForQuestion(final DecisionTree.Question question, final String css) {
        if (mBmapWorkflowIcons == null) {
            final String cacheFileIcons = getCacheFileUri(CACHE_FILE_WORKFLOW_ICONS);
            mBmapWorkflowIcons = BitmapFactory.decodeFile(cacheFileIcons);
        }

        if (mBmapExampleIcons == null) {
            final String cacheFileIcons = getCacheFileUri(CACHE_FILE_EXAMPLE_ICONS);
            mBmapExampleIcons = BitmapFactory.decodeFile(cacheFileIcons);
        }


        for (final DecisionTree.Answer answer : question.getAnswers()) {
            //Get the icon for the answer:
            final String iconName = answer.getIcon();
            loadIconBasedOnCss(mBmapWorkflowIcons, css, iconName, false);
            getExampleImages(question, css, answer);
        }

        for (final DecisionTree.Checkbox checkbox : question.getCheckboxes()) {
            final String iconName = checkbox.getIcon();
            loadIconBasedOnCss(mBmapWorkflowIcons, css, iconName, false);
            getExampleImages(question, css, checkbox);
        }
    }


    private void getExampleImages(DecisionTree.Question question, String css, DecisionTree.BaseButton answer) {
        //Get the example images for the answer or checkbox:
        for (int i = 0; i < answer.getExamplesCount(); i++) {
            final String exampleIconName = answer.getExampleIconName(question.getId(), i);
            loadIconBasedOnCss(mBmapExampleIcons, css, exampleIconName, true);
        }
    }

    private void onCssDownloaded() {
        final String cacheFileUri = getCacheFileUri(CACHE_FILE_CSS);
        final String css = getFileContents(cacheFileUri);

        mWorkflowIcons.evictAll();
        mExampleIcons.evictAll();

        for (final DecisionTree decisionTree : mDecisionTrees.values()) {
            final List<DecisionTree.Question> questions = decisionTree.getAllQuestions();
            boolean allSucceeded = true;
            for (final DecisionTree.Question question : questions) {
                cacheIconsForQuestion(question, css);
            }
        }
    }

    private String getCacheIconFileUri(final String cssName) {
        return getCacheFileUri(ICON_FILE_PREFIX + cssName);
    }

    private String createCacheIconFile(final String cssName) {
        return createCacheFile(ICON_FILE_PREFIX + cssName);
    }

    private String getCacheFileUri(final String cacheId) {
        final File file = new File(mCacheDir.getPath(), cacheId);
        return file.getAbsolutePath();
    }

    private String createCacheFile(final String cacheId) {
        final File file = new File(mCacheDir.getPath(), cacheId);
        try {
            file.createNewFile();
        } catch (final IOException e) {
            //TODO: Let the caller catch this?
            Log.error("Could not create cache file.", e);
            return null;
        }

        return file.getAbsolutePath();
    }

    String getFileContents(final String fileUri) {
        File file = new File(fileUri);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            InputStreamReader isr = null;
            try {
                isr = new InputStreamReader(fis, Utils.STRING_ENCODING);

                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(isr);

                    final StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }

                    return sb.toString();
                } catch (final IOException e) {
                    //TODO: Let the caller catch this?
                    Log.error("getFileContents(): IOException", e);
                    return "";
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (final IOException e) {
                            Log.error("getFileContents(): exception while closing bufferedReader", e);
                        }
                    }
                }
            } catch (final IOException e) {
                //TODO: Let the caller catch this?
                Log.error("getFileContents(): IOException", e);
                return "";
            } finally {
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (final IOException e) {
                        Log.error("getFileContents(): exception while closing isr", e);
                    }
                }
            }
        } catch (final IOException e) {
            //TODO: Let the caller catch this?
            Log.error("getFileContents(): IOException", e);
            return "";
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (final IOException e) {
                    Log.error("getFileContents(): exception while closing fis", e);
                }
            }
        }
    }


    // This little helper function is instead of using a whole CSS parser,
    // in the absence of an easy choice of CSS parser.
    // http://sourceforge.net/projects/cssparser/ doesn't seem to be usable on Android because
    // Android's org.w3c.dom doesn't have the css package, with classes such as CSSStyleSheet.
    void loadIconBasedOnCss(final Bitmap icons, final String css, final String cssName, boolean isExampleIcon) {
        if (icons == null) {
            Log.error("loadIconBasedOnCss(): icons is null.");
            return;
        }

        if (TextUtils.isEmpty(css)) {
            Log.error("loadIconBasedOnCss(): css is empty.");
            return;
        }

        if (TextUtils.isEmpty(cssName)) {
            Log.error("loadIconBasedOnCss(): cssName is empty.");
            return;
        }

        if (mWorkflowIcons.get(cssName) != null) {
            //Avoid getting it again.
            return;
        }

        Pattern p;

        String prefix;
        if (isExampleIcon) {
            prefix = "\\.example-thumbnail\\.";
        } else {
            prefix = "a\\.workflow-";
        }


        if(!attemptLoadIconFromCssWithPosition(icons, css, cssName, prefix)) {
            if(!attemptLoadIconFromCssWithSingleFile(icons, css, cssName, prefix)) {
                Log.error("loadIconBasedOnCss(): No icons found for cssName=" + cssName);
            }
        }
    }

    private boolean attemptLoadIconFromCssWithPosition(Bitmap icons, String css, String cssName, String prefix) {
        final String regex = prefix + cssName + "\\{background-position:(-?[0-9]+)(px)? (-?[0-9]+)(px)?\\}";
        final Pattern p = Pattern.compile(regex);
        //p = Pattern.compile("a.workflow-" + cssName);


        final Matcher m = p.matcher(css);
        boolean someFound = false;
        while (m.find()) {
            if (m.groupCount() < 4) { //Doesn't include the 0 group.
                Log.info("Regex error. Unexpected groups count:" + m.groupCount());
            } else {
                final String xStr = m.group(1); //Group 0 is the whole region.
                final String yStr = m.group(3);

                final int x = -(Integer.parseInt(xStr)); //Change negative (CSS) to positive (Bitmap).
                final int y = -(Integer.parseInt(yStr)); //Change negative (CSS) to positive (Bitmap).

                //TODO: Avoid hard-coding the 100px, 100px here:
                try {
                    final Bitmap bmapIcon = Bitmap.createBitmap(icons, x, y, 100, 100);
                    cacheWorkflowIcon(cssName, bmapIcon);
                    someFound = true;
                } catch (final IllegalArgumentException ex) {
                    //We catch this IllegalArgumentException to avoid letting the CSS crash our app
                    //just by having a wrong value.
                    Log.error("IllegalArgumentException from createBitmap() for iconName=" + cssName + ", x=" + x + ", y=" + y + ", icons.width=" + icons.getWidth() + ", icons.height=" + icons.getHeight());
                }
            }
        }

        return someFound;
    }

    private boolean attemptLoadIconFromCssWithSingleFile(Bitmap icons, String css, String cssName, String prefix) {
        final String regex = prefix + cssName + "\\{background:url\\(\\\"(\\S*)\\\"\\) center/cover\\}";
        final Pattern p = Pattern.compile(regex);
        //p = Pattern.compile("a.workflow-" + cssName);


        final Matcher m = p.matcher(css);
        boolean someFound = false;
        while (m.find()) {
            if (m.groupCount() < 1) { //Doesn't include the 0 group.
                Log.info("Regex error. Unexpected groups count:" + m.groupCount());
                continue;
            } else {
                final String filename = m.group(1); //Group 0 is the whole pattern.
                final String path = com.murrayc.galaxyzoo.app.Config.STATIC_SERVER + filename;

                //Cache the file locally so we don't need to get it over the network next time:
                //TODO: Use the cache from the volley library?
                //See http://blog.wittchen.biz.pl/asynchronous-loading-and-caching-bitmaps-with-volley/
                final String cacheFileUri = createCacheIconFile(cssName);
                try {
                    if(!HttpUtils.cacheUriToFileSync(getContext(), mRequestQueue, path, cacheFileUri)) {
                        Log.error("readIconsFileSync(): cacheUriToFileSync() failed.");
                    }
                } catch (final HttpUtils.FileCacheException e) {
                    Log.error("readIconsFileSync: Exception from HttpUtils.cacheUriToFileSync", e);
                    continue;
                }

                final Bitmap bmapIcon = BitmapFactory.decodeFile(cacheFileUri);
                if (bmapIcon == null) {
                    Log.error("attemptLoadIconFromCssWithSingleFile(): Could not decode image: " + path);
                    continue;
                }

                //We check for nulls because LruCache throws NullPointerExceptions on null
                //keys or values.
                if (!TextUtils.isEmpty(cssName) && (bmapIcon != null)) {
                    mWorkflowIcons.put(cssName, bmapIcon);
                    someFound = true;
                }
            }
        }

        return someFound;
    }

    /** Cache the file in the file system and remember it.
     *
     * @param cssName
     * @param bmapIcon
     */
    /*
    private void cacheWorkflowIcon(final String cssName, final Bitmap bmapIcon) {
        //Cache the file locally so we don't need to get it over the network next time:
        //TODO: Use the cache from the volley library?
        //See http://blog.wittchen.biz.pl/asynchronous-loading-and-caching-bitmaps-with-volley/
        final String cacheFileUri = createCacheIconFile(cssName);
        cacheBitmapToFile(bmapIcon, cacheFileUri);

        //We check for nulls because LruCache throws NullPointerExceptions on null
        //keys or values.
        if (!TextUtils.isEmpty(cssName) && (bmapIcon != null)) {
            mWorkflowIcons.put(cssName, bmapIcon);
        }
    }

    private void cacheBitmapToFile(final Bitmap bmapIcon, final String cacheFileUri) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStream fout = null;
        try {
            fout = new FileOutputStream(cacheFileUri);
            bmapIcon.compress(Bitmap.CompressFormat.PNG, 100, stream);
            final byte[] byteArray = stream.toByteArray();
            fout.write(byteArray);
        } catch (final IOException e) {
            //TODO: Let the caller catch this?
            Log.error("cacheBitmapToFile(): Exception while caching icon bitmap.", e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (final IOException e) {
                    Log.error("cacheBitmapToFile(): Exception while closing fout.", e);
                }
            }
        }

        if (stream != null) {
            try {
                stream.close();
            } catch (final IOException e) {
                Log.error("cacheBitmapToFile(): Exception while closing stream.", e);
            }
        }
    }
    */

    public Bitmap getIcon(final String iconName) {
        //Avoid a NullPointerException from LruCache.get() if we pass a null key.
        if (TextUtils.isEmpty(iconName)) {
            return null;
        }

        Bitmap result = mWorkflowIcons.get(iconName);

        //Reload it if it is no longer in the cache:
        if (result == null) {
            reloadIcon(iconName, mWorkflowIcons);
            result = mWorkflowIcons.get(iconName);
        }

        return result;
    }

    private Context getContext() {
        return mContext;
    }
}