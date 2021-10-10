package com.nokia.mid.ui;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import javax.microedition.util.ContextHolder;

public abstract class Clipboard {
	private static ClipboardManager clipboardmgr;

	public static void copyToClipboard(String text) {
		if(clipboardmgr == null) {
			clipboardmgr = (ClipboardManager) ContextHolder.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		}
		ClipData clip = ClipData.newPlainText("", text);
		clipboardmgr.setPrimaryClip(clip);
	}

	public static String copyFromClipboard() {
		if(clipboardmgr == null) {
			clipboardmgr = (ClipboardManager) ContextHolder.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		}
		String text = null;
		if (!clipboardmgr.hasPrimaryClip()) {
			return "";
		}
		if (!clipboardmgr.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
			return "";
		}
		ClipData.Item item = clipboardmgr.getPrimaryClip().getItemAt(0);
		return item.getText().toString();
	}
}
