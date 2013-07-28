package com.kskkbys.loop.ui.widget;

import com.kskkbys.loop.logger.KLog;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class HorizontalAutoScrollView extends HorizontalScrollView {

	private static final String TAG = HorizontalScrollView.class.getSimpleName();
	
	public HorizontalAutoScrollView(Context context) {
		super(context);
		startAutoScroll();
	}

	public HorizontalAutoScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		startAutoScroll();
	}

	public HorizontalAutoScrollView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		startAutoScroll();
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// Does not intercept touch event
		KLog.v(TAG, "onTouchEvent");
		return false;
	}*/

	private void startAutoScroll() {
		final long SCROLL_INTERVAL = 60 * 1000;
		new CountDownTimer(SCROLL_INTERVAL, 20) {
			public void onTick(long millisUntilFinished) { 
				long x = getRight() * (SCROLL_INTERVAL - millisUntilFinished) / SCROLL_INTERVAL;
				// KLog.v(TAG, "scroll x = " + x);
				scrollTo((int)x, 0);
			}
			public void onFinish() { 
			}
		}.start();
	}
}
