package com.kskkbys.loop.dialog;

import com.kskkbys.loop.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Alert dialog fragment
 */
public class AlertDialogFragment extends DialogFragment {

	/**
	 * Create new instance
	 * @param resId
	 * @return
	 */
	public static AlertDialogFragment newInstance(int resId) {
		AlertDialogFragment fragment = new AlertDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putInt("resId", resId);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		int resId = getArguments().getInt("resId");
		builder.setMessage(resId);
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.loop_ok, null);
		return builder.create();
	}

}
