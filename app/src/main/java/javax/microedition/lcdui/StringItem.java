/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017 Nikita Shakarun
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

package javax.microedition.lcdui;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class StringItem extends Item {
	private String text;
	private TextView textview;
	private final int appearanceMode;

	private final SimpleEvent msgSetText = new SimpleEvent() {
		@Override
		public void process() {
			if (appearanceMode == HYPERLINK && text != null) {
				SpannableStringBuilder s = new SpannableStringBuilder(text);
				s.setSpan(new URLSpan(text), 0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
				textview.setText(s);
			} else {
				textview.setText(text);
			}
		}
	};

	public StringItem(String label, String text) {
		this(label, text, PLAIN);
	}

	public StringItem(String label, String text, int appearanceMode) {
		setLabel(label);
		setText(text);
		this.appearanceMode = appearanceMode;
	}

	public void setText(String text) {
		this.text = text;

		if (textview != null) {
			ViewHandler.postEvent(msgSetText);
		}
	}

	public String getText() {
		return text;
	}

	public Font getFont() {
		return Font.getDefaultFont();
	}

	public void setFont(Font font) {
	}

	public int getAppearanceMode() {
		return appearanceMode;
	}

	@Override
	public View getItemContentView() {
		if (textview == null) {
			Context context = ContextHolder.getActivity();

			if (appearanceMode == BUTTON) {
				textview = new AppCompatButton(context);
			} else {
				textview = new AppCompatTextView(context);
			}

			textview.setTextAppearance(context, android.R.style.TextAppearance_Small);
			if (appearanceMode == HYPERLINK && text != null) {
				SpannableStringBuilder s = new SpannableStringBuilder(text);
				s.setSpan(new URLSpan(text), 0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
				textview.setText(s);
			} else {
				textview.setText(text);
			}
			textview.setOnClickListener(v -> fireDefaultCommandAction());
		}

		return textview;
	}

	@Override
	public void clearItemContentView() {
		textview = null;
	}
}