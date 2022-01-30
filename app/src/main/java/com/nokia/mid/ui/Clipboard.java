/*
 * Copyright 2021 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
