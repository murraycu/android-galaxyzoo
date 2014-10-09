package com.murrayc.galaxyzoo.app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by murrayc on 10/9/14.
 */
public class ZooLinearLayout extends LinearLayout {
    public ZooLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final int measuredHeight = getMeasuredHeight();
        Log.info("ZooLinearLayout.onLayout(): measuredHeight=" + measuredHeight);
    }
}
