package com.kskkbys.loop.dialog;

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

	private Context mContext;
	private int mMessageId;
	private OnClickListener mListener;

	/**
	 * Create new instance
	 * @param context
	 * @param resId
	 * @param listener
	 * @return
	 */
	public static AlertDialogFragment newInstance(Context context, int resId, OnClickListener listener) {
		AlertDialogFragment fragment = new AlertDialogFragment();
		fragment.mContext = context;
		fragment.mMessageId = resId;
		fragment.mListener = listener;
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mMessageId);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", mListener);
		return builder.create();
	}

}
