package com.kskkbys.loop;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Application;

public class LoopApplication extends Application {
	
	private boolean mIsFirstLaunch;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		//
		mIsFirstLaunch = true;
		
		// GA
		EasyTracker.getInstance().setContext(this.getApplicationContext());
	}
	
	public boolean isFirstLaunch() {
		if (mIsFirstLaunch) {
			mIsFirstLaunch = false;
			return true;
		}
		return false;
	}

}
