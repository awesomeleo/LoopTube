package com.kskkbys.loop;

import com.kskkbys.loop.playlist.BlackList;

import android.app.Application;

public class LoopApplication extends Application {
	
	private boolean mIsFirstLaunch;
	
	@Override
	public void onCreate() {
		super.onCreate();
		//
		mIsFirstLaunch = true;
		// Initialize
		BlackList.getInstance().initialize(getApplicationContext());
	}
	
	public boolean isFirstLaunch() {
		if (mIsFirstLaunch) {
			mIsFirstLaunch = false;
			return true;
		}
		return false;
	}

}
