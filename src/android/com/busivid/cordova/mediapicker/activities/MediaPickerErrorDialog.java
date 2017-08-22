package com.busivid.cordova.mediapicker.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Create dialog for media picker module. Should only use in this module.
 */
public class MediaPickerErrorDialog extends DialogFragment {
	private String mMessage;
	private OnClickListener mOnPositionClickListener;

	public static MediaPickerErrorDialog newInstance(String msg) {
		MediaPickerErrorDialog dialog = new MediaPickerErrorDialog();
		Bundle bundle = new Bundle();
		bundle.putString("msg", msg);
		dialog.setArguments(bundle);
		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMessage = getArguments().getString("msg");
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setMessage(mMessage).setPositiveButton(getResources().getIdentifier("ok", "string", getPackageName()), mOnPositionClickListener).create();
	}

	public void setOnOKClickListener(OnClickListener mOnClickListener) {
		this.mOnPositionClickListener = mOnClickListener;
	}
}
