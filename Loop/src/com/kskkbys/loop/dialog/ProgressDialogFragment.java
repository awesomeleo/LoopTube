package com.kskkbys.loop.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Progress dialog fragment
 */
public class ProgressDialogFragment extends DialogFragment {

	private int mMessageId;

	public static ProgressDialogFragment newInstance(int resId) {
		ProgressDialogFragment fragment = new ProgressDialogFragment();
		fragment.mMessageId = resId;
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		ProgressDialog dialog = new ProgressDialog(getActivity());

		dialog.setMessage(getText(mMessageId));
		dialog.setCancelable(false);
		return dialog;
	}

}
