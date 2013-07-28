package com.kskkbys.loop.ui.widget;

import com.kskkbys.loop.logger.KLog;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class HorizontalAutoScrollView extends HorizontalScrollView {

	private static final String TAG = HorizontalScrollView.class.getSimpleName();
	
	private static final long SCROLL_INTERVAL = 60 * 1000;
	
	private CountDownTimer mTimer;
	
	
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

	/**
	 * Start auto scroll
	 */
	public void startAutoScroll() {
		//Start timer from initial position
		scrollTo(0, 0);
		mTimer = new CountDownTimer(SCROLL_INTERVAL, 20) {
			public void onTick(long millisUntilFinished) {
				KLog.v(TAG, "onTick");
				long x = getRight() * (SCROLL_INTERVAL - millisUntilFinished) / SCROLL_INTERVAL;
				scrollTo((int)x, 0);
			}
			public void onFinish() {
				KLog.v(TAG, "Finished to scroll. Restart.");
				// startAutoScroll();
			}
		};
		mTimer.start();
	}
	
	/**
	 * Stop auto scroll
	 */
	public void stopAutoScroll() {
		mTimer.cancel();
	}
}
