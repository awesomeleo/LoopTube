package com.kskkbys.loop.dialog;

import com.kskkbys.loop.logger.KLog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Progress dialog fragment
 */
public class ProgressDialogFragment extends DialogFragment {

	private static final String TAG = ProgressDialogFragment.class.getSimpleName();

	public static ProgressDialogFragment newInstance(int resId) {
		KLog.v(TAG, "newInstance");
		ProgressDialogFragment fragment = new ProgressDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putInt("resId", resId);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);

		KLog.v(TAG, "onCreateDialog");
		int resId = getArguments().getInt("resId");

		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(getText(resId));
		dialog.setCancelable(false);
		return dialog;
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

}
