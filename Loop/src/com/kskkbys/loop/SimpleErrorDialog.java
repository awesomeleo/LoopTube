package com.kskkbys.loop;

import android.app.AlertDialog;
import android.content.Context;

public class SimpleErrorDialog {
	
	public static void show(Context context, int resId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(resId);
		builder.setPositiveButton(R.string.loop_ok, null);
		builder.create().show();
	}

}
