/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2019 Nikita Shakarun
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
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

import javax.microedition.lcdui.event.SimpleEvent;

class TextFieldImpl {
	private EditText textview;

	private String text;
	private int maxSize;
	private int constraints;

	private SimpleEvent msgSetText = new SimpleEvent() {
		@Override
		public void process() {
			textview.setText(text);
		}
	};

	void setString(String text) {
		if (text != null && text.length() > maxSize) {
			throw new IllegalArgumentException("text length exceeds max size");
		}

		if (text != null) {
			this.text = text;
		} else {
			this.text = "";
		}

		if (textview != null) {
			ViewHandler.postEvent(msgSetText);
		}
	}

	void insert(String src, int pos) {
		String tmp = new StringBuilder(text).insert(pos, src).toString();
		setString(tmp);
	}

	String getString() {
		return text;
	}

	int size() {
		return text.length();
	}

	int setMaxSize(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("max size must be > 0");
		}

		this.maxSize = maxSize;

		if (textview != null) {
			textview.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxSize)});
		}

		return maxSize;
	}

	int getMaxSize() {
		return maxSize;
	}

	void setConstraints(int constraints) {
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
			}
		}
	}

	int getConstraints() {
		return constraints;
	}

	int getChars(char[] data) {
		text.getChars(0, text.length(), data, 0);
		return text.length();
	}

	int getCaretPosition() {
		if (textview != null) {
			return textview.getSelectionEnd();
		} else {
			return 0;
		}
	}

	void delete(int offset, int length) {
		String tmp = new StringBuilder(text).delete(offset, offset + length).toString();
		setString(tmp);
	}

	EditText getView(Context context, Item item) {
		if (textview == null) {
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

			if (item != null) {
				textview.setOnFocusChangeListener((v, hasFocus) -> {
					if (!hasFocus) item.notifyStateChanged();
				});
			} else {
				textview.setLayoutParams(new LinearLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				textview.setGravity(Gravity.TOP);
			}
		}
		return textview;
	}

	void clearScreenView() {
		textview = null;
	}
}
