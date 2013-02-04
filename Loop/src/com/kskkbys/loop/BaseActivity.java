package com.kskkbys.loop;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.flurry.android.FlurryAgent;
import com.kskkbys.loop.dialog.ProgressDialogFragment;

/**
 * Base activity of all activities which extends SherlockActivity to support ActionBar.
 * Flurry API calling and DialogFragment handling.
 */
public class BaseActivity extends SherlockFragmentActivity {
	
	private static final String TAG = BaseActivity.class.getSimpleName();
	private static final String apiKey = "GNRQ5VMSSVJ2WR2W38SD";
	
	private ProgressDialogFragment mProgressDialogFragment;
	private boolean mShowDialog;
	
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mShowDialog = false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mShowDialog = true;
	}
	

	protected void showProgress(int resId) {
		Log.v(TAG, "showProgress");
		// Remove prev fragment
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Show dialog fragment
		if (mProgressDialogFragment == null && mShowDialog) {
			mProgressDialogFragment = ProgressDialogFragment.newInstance(resId);
			mProgressDialogFragment.show(ft, "dialog");
		}
	}

	protected void dismissProgress() {
		Log.v(TAG, "dismissProgress");
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		if (mProgressDialogFragment != null) {
			mProgressDialogFragment.dismiss();
			mProgressDialogFragment = null;
		}
	}

}
