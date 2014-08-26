package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class IconsCache {
    //TODO: Generate these automatically, making sure they are unique:
    public static final String CACHE_FILE_WORKFLOWICONS = "workflowicons";
    public static final String CACHE_FILE_CSS = "css";

    private final DecisionTree mDecisionTree;
    private final File mCacheDir;
    private final Hashtable<String, Bitmap> mIcons = new Hashtable<String, Bitmap>();
    private Bitmap mBmapIcons = null;

    /**
     * This does network IO so it should not be used in the UI's main thread.
     * For instance, do this in an AsyncTask, as in Singleton.init().
     *
     * @param context
     * @param decisionTree
     */
    public IconsCache(final Context context, final DecisionTree decisionTree) {
        this.mDecisionTree = decisionTree;
        
        mCacheDir = context.getExternalCacheDir();
        readIconsFileSync(Config.ICONS_URI, CACHE_FILE_WORKFLOWICONS);
        readCssFileSync(Config.ICONS_CSS_URI, CACHE_FILE_CSS);
        mBmapIcons = null;
    }

    private void readIconsFileSync(final String uriStr, final String cacheId) {
        final String cacheFileUri = getCacheFileUri(cacheId);
        HttpUtils.cacheUriToFileSync(uriStr, cacheFileUri);
    }

    private void readCssFileSync(final String uriStr, final String cacheId) {
        final String cacheFileUri = getCacheFileUri(cacheId);
        HttpUtils.cacheUriToFileSync(uriStr, cacheFileUri);
        onCssDownloaded();
    }

    private void cacheIconsForQuestion(final DecisionTree.Question question, final String css) {
        final String cacheFileUri = getCacheFileUri(CACHE_FILE_CSS); //Avoid repeated calls to this.

        if (mBmapIcons == null) {
            final String cacheFileIcons = getCacheFileUri(CACHE_FILE_WORKFLOWICONS); //Avoid repeated calls to this.
            mBmapIcons = BitmapFactory.decodeFile(cacheFileIcons);
        }

        for (final DecisionTree.Answer answer : question.answers) {
            final String iconName = answer.getIcon();
            getIconPositionFromCss(mBmapIcons, css, iconName);

            //Recurse:
            final DecisionTree.Question nextQuestion = mDecisionTree.getNextQuestionForAnswer(question.getId(), answer.getId());
            if (nextQuestion != null) {
                cacheIconsForQuestion(nextQuestion, css);
            }
        }

        for (final DecisionTree.Checkbox checkbox : question.checkboxes) {
            final String iconName = checkbox.getIcon();
            getIconPositionFromCss(mBmapIcons, css, iconName);
        }
    }

    private void onCssDownloaded() {
        final String cacheFileUri = getCacheFileUri(CACHE_FILE_CSS);
        final String css = getFileContents(cacheFileUri);

        // Recurse through the questions, looking at each icon:
        final DecisionTree.Question question = mDecisionTree.getFirstQuestion();
        cacheIconsForQuestion(question, css);
    }

    private String getCacheFileUri(final String cacheId) {
        final File file = new File(mCacheDir.getPath(), cacheId);
        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.error("Could not create cache file.", e);
            return null;
        }

        return file.getAbsolutePath();
    }

    String getFileContents(final String fileUri) {
        File file = new File(fileUri);
        try {
            final FileInputStream fis = new FileInputStream(file);
            final InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            final BufferedReader bufferedReader = new BufferedReader(isr);
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            fis.close();
            return sb.toString();
        } catch (FileNotFoundException e) {
            Log.error("readFile failed.", e);
            return "";
        } catch (UnsupportedEncodingException e) {
            Log.error("readFile failed.", e);
            return "";
        } catch (IOException e) {
            Log.error("readFile failed.", e);
            return "";
        }
    }


    // This little helper function is instead of using a whole CSS parser,
    // in the absence of an easy choice of CSS parser.
    // http://sourceforge.net/projects/cssparser/ doesn't seem to be usable on Android because
    // Android's org.w3c.dom doesn't have the css package, with classes such as CSSStyleSheet.
    void getIconPositionFromCss(final Bitmap icons, final String css, final String cssName) {
        if (mIcons.containsKey(cssName)) {
            //Avoid getting it again.
            return;
        }

        Pattern p = null;
        try {
            p = Pattern.compile("a.workflow-" + cssName + "\\{background-position:(-?[0-9]+)(px)? (-?[0-9]+)(px)?\\}");
            //p = Pattern.compile("a.workflow-" + cssName);
        } catch (PatternSyntaxException e) {
            Log.error("Regex error", e);
            return;
        }

        final Matcher m = p.matcher(css);
        while (m.find()) {
            if (m.groupCount() < 4) {
                Log.info("Regex error. Unexpected groups count:" + m.groupCount());
            } else {
                final String xStr = m.group(1);
                final String yStr = m.group(3);

                final int x = Integer.parseInt(xStr);
                final int y = -(Integer.parseInt(yStr)); //Change negative (CSS) to positive (Bitmap).
                Log.info("debugMatch: x=" + x + ", y=" + y);

                //TODO: Avoid hard-coding the 100px, 100px here:
                final Bitmap bmapIcon = Bitmap.createBitmap(icons, x, y, 100, 100);
                mIcons.put(cssName, bmapIcon);
            }
        }
    }

    public Bitmap getIcon(final String iconName) {
        return mIcons.get(iconName);
    }
}