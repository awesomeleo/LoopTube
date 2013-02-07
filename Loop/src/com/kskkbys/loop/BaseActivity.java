package com.kskkbys.loop;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.kskkbys.loop.dialog.AlertDialogFragment;
import com.kskkbys.loop.dialog.ProgressDialogFragment;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;

/**
 * Base activity of all activities which extends SherlockActivity to support ActionBar.
 * Flurry API calling and DialogFragment handling.
 */
public class BaseActivity extends SherlockFragmentActivity {

	private static final String TAG = BaseActivity.class.getSimpleName();

	private ProgressDialogFragment mProgressDialogFragment;
	
	/**
	 * This flag indicates whether dialogs can be shown or not.
	 */
	private boolean mShowDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_Sherlock);
		mShowDialog = true;
	}

	@Override
	public void onStart() {
		super.onStart();
		FlurryLogger.onStartSession(this);
		FlurryLogger.onPageView();
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryLogger.onEndSession(this);
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

	protected void showAlert(int resId) {
		KLog.v(TAG, "showAlert");
		// Remove prev fragment
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Show dialog fragment
		if (mShowDialog) {
			AlertDialogFragment fragment = AlertDialogFragment.newInstance(this, resId);
			fragment.setCancelable(false);
			fragment.show(ft, "dialog");
		}
	}

	protected void showProgress(int resId) {
		KLog.v(TAG, "showProgress");
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
			mProgressDialogFragment.setCancelable(false);
			mProgressDialogFragment.show(ft, "dialog");
		}
	}
	
	protected void dismissProgress() {
		KLog.v(TAG, "dismissProgress");
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
