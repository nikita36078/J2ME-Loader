package com.nokia.mid.ui;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import javax.microedition.util.ContextHolder;

public abstract class Clipboard {
	private static final ClipboardManager clipboardmgr;

	static {
		clipboardmgr = (ClipboardManager) ContextHolder.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
	}

	public static void copyToClipboard(String text) {
		ClipData clip = ClipData.newPlainText("", text);
		clipboardmgr.setPrimaryClip(clip);
	}

	public static String copyFromClipboard() {
		if (!clipboardmgr.hasPrimaryClip()) {
			return "";
		}
		if (!clipboardmgr.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
			// Not text
			return "";
		}
		ClipData.Item item = clipboardmgr.getPrimaryClip().getItemAt(0);
		return item.getText().toString();
	}
}
