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
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;

import javax.microedition.lcdui.event.SimpleEvent;

public class TextBox extends Screen {
	private ScrollView scrollview;

	private String text;
	private EditText textview;
	private int maxSize;
	private int constraints;

	private SimpleEvent msgSetText = new SimpleEvent() {
		@Override
		public void process() {
			textview.setText(text);
		}
	};

	public TextBox(String title, String text, int maxSize, int constraints) {
		setTitle(title);
		setMaxSize(maxSize);
		setConstraints(constraints);
		setString(text);
	}

	public void setString(String text) {
		if (text != null && text.length() > maxSize) {
			throw new IllegalArgumentException("text length exceeds max size");
		}

		this.text = text;

		if (textview != null) {
			ViewHandler.postEvent(msgSetText);
		}
	}

	public void insert(String src, int pos) {
		this.text = new StringBuilder(getString()).insert(pos, src).toString();

		setString(text);
	}

	public String getString() {
		return text;
	}

	public int size() {
		return getString().length();
	}

	public int setMaxSize(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("max size must be > 0");
		}

		this.maxSize = maxSize;

		if (textview != null) {
			textview.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxSize)});
		}

		return maxSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setConstraints(int constraints) {
		this.constraints = constraints;

		if (textview != null) {
			int inputtype;

			switch (constraints & TextField.CONSTRAINT_MASK) {
				default:
				case TextField.ANY:
					inputtype = InputType.TYPE_CLASS_TEXT;
					break;

				case TextField.EMAILADDR:
					inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
					break;

				case TextField.NUMERIC:
					inputtype = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
					break;

				case TextField.PHONENUMBER:
					inputtype = InputType.TYPE_CLASS_PHONE;
					break;

				case TextField.URL:
					inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
					break;

				case TextField.DECIMAL:
					inputtype = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL;
					break;
			}

			if ((constraints & TextField.PASSWORD) != 0 ||
					(constraints & TextField.SENSITIVE) != 0) {
				inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}

			if ((constraints & TextField.UNEDITABLE) != 0) {
				inputtype = InputType.TYPE_NULL;
			}

			if ((constraints & TextField.NON_PREDICTIVE) != 0) {
				inputtype |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			}

			if ((constraints & TextField.INITIAL_CAPS_WORD) != 0) {
				inputtype |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
			}

			if ((constraints & TextField.INITIAL_CAPS_SENTENCE) != 0) {
				inputtype |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
			}

			textview.setInputType(inputtype);
			if ((constraints & TextField.CONSTRAINT_MASK) == TextField.ANY) {
				textview.setSingleLine(false);
				textview.setMaxLines(5);
			}
		}
	}

	public int getConstraints() {
		return constraints;
	}

	public void setInitialInputMode(String characterSubset) {
	}

	public void setChars(char[] data, int offset, int length) {
		setString(new String(data, offset, length));
	}

	@Override
	public View getScreenView() {
		if (scrollview == null) {
			Context context = getParentActivity();

			textview = new EditText(context);

			setMaxSize(maxSize);
			setConstraints(constraints);
			setString(text);

			textview.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					text = s.toString();
				}
			});
			scrollview = new ScrollView(context);
			scrollview.addView(textview);
		}

		return scrollview;
	}

	@Override
	public void clearScreenView() {
		scrollview = null;
		textview = null;
	}

	public int getCaretPosition() {
		if (textview != null) {
			return textview.getSelectionEnd();
		} else {
			return -1;
		}
	}
}
