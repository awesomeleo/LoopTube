package com.kskkbys.loop;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Application;

public class LoopApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		// GA
		EasyTracker.getInstance().setContext(this.getApplicationContext());
	}

}
