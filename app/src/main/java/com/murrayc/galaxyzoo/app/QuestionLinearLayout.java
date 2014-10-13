package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by murrayc on 10/9/14.
 */
public class QuestionLinearLayout extends LinearLayout {

    //A map of number-of-rows-of-icons to max height experienced.
    private Map<Integer, Integer> mMaxHeightsExperienced = new HashMap<>();
    int mCurrentRowsCountForMaxHeight = 0;

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
        if ((measuredHeight > 0) && (getMaximumHeightExperienced(mCurrentRowsCountForMaxHeight) < measuredHeight)) {
            mMaxHeightsExperienced.put(mCurrentRowsCountForMaxHeight, measuredHeight);
        }

        //Log.info("ZooLinearLayout.onLayout(): measuredHeight=" + measuredHeight);
    }

    public void setRowsCountForMaxHeightExperienced(int rowsCount) {
        mCurrentRowsCountForMaxHeight = rowsCount;
    }

    public int getMaximumHeightExperienced(int rowsCount) {
        if (!mMaxHeightsExperienced.containsKey(rowsCount)) {
            return 0;
        }

        return mMaxHeightsExperienced.get(rowsCount);
    }
}
