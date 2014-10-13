package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by murrayc on 10/9/14.
 */
public class QuestionLinearLayout extends LinearLayout {
    private int mMaxHeightExperienced = 0;

    public QuestionLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        //Remember the greatest height that this has fragment has ever requested,
        //so we can try to always ask for at least that,
        //to avoid the other parts of the UI moving around too much.
        final int measuredHeight = getMeasuredHeight();
        if ((measuredHeight > 0) && (mMaxHeightExperienced < measuredHeight)) {
            mMaxHeightExperienced = measuredHeight;
        }

        //Log.info("ZooLinearLayout.onLayout(): measuredHeight=" + measuredHeight);
    }

    public int getMaximumHeightExperienced() {
        return mMaxHeightExperienced;
    }
}
