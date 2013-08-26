package com.kskkbys.loop.ui;

import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

import com.kskkbys.loop.dialog.AlertDialogFragment;
import com.kskkbys.loop.dialog.ProgressDialogFragment;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;

/**
 * Base activity of all activities which extends SherlockActivity to support ActionBar.
 * Flurry API calling and DialogFragment handling.
 */
public class BaseActivity extends ActionBarActivity {

	private static final String TAG = BaseActivity.class.getSimpleName();
	
	private static final String TAG_ALERT = "alert";
	private static final String TAG_PROGRESS = "progress";
	
	/**
	 * This flag indicates whether dialogs can be shown or not.
	 */
	private boolean mCanShowDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCanShowDialog = true;
	}
	
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryLogger.onStartSession(this);
		FlurryLogger.onPageView();
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryLogger.onEndSession(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Before calling onSavedInstanceState, dismiss Dialog
		mCanShowDialog = false;
		dismissProgress();
		//
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCanShowDialog = true;
	}

	public void showAlert(int resId, OnClickListener listener) {
		KLog.v(TAG, "showAlert");
		// Remove prev fragment
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_ALERT);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Show dialog fragment
		if (mCanShowDialog) {
			AlertDialogFragment fragment = AlertDialogFragment.newInstance(resId);
			fragment.setCancelable(false);
			fragment.show(ft, TAG_ALERT);
		}
	}

	public void showProgress(int resId) {
		KLog.v(TAG, "showProgress");
		// Show dialog fragment
		if (mCanShowDialog) {
			ProgressDialogFragment frag = ProgressDialogFragment.newInstance(resId);
			frag.setCancelable(false);
			//frag.show(ft, TAG_PROGRESS);
			frag.show(getSupportFragmentManager(), TAG_PROGRESS);
		}
	}
	
	public void dismissProgress() {
		KLog.v(TAG, "dismissProgress");
		ProgressDialogFragment prev = (ProgressDialogFragment)getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS);
		if (prev != null) {
			KLog.v(TAG, "Dismissed");
			prev.dismissAllowingStateLoss();
		} else {
			KLog.v(TAG, "already dismissed");
		}
	}

}
