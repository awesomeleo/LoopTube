package com.kskkbys.loop;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;

/**
 * Base activity of all activities
 * Implemented Google Analytics
 */
public class BaseActivity extends Activity {
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

}
