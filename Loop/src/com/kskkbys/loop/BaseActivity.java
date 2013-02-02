package com.kskkbys.loop;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.flurry.android.FlurryAgent;

/**
 * Base activity of all activities which extends SherlockActivity to support ActionBar.
 * Implemented Google Analytics.
 */
public class BaseActivity extends SherlockFragmentActivity {
	
	private static final String apiKey = "GNRQ5VMSSVJ2WR2W38SD";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_Sherlock);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, apiKey);
		FlurryAgent.onPageView();
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

}
