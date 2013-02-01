package com.kskkbys.loop;

import android.app.Application;

public class LoopApplication extends Application {
	
	private boolean mIsFirstLaunch;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		//
		mIsFirstLaunch = true;
	}
	
	public boolean isFirstLaunch() {
		if (mIsFirstLaunch) {
			mIsFirstLaunch = false;
			return true;
		}
		return false;
	}

}
