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
import android.view.View;

public class TextBox extends Screen {
	private final TextFieldImpl textField;

	public TextBox(String title, String text, int maxSize, int constraints) {
		textField = new TextFieldImpl();
		setTitle(title);
		setMaxSize(maxSize);
		setConstraints(constraints);
		setString(text);
	}

	public void setString(String text) {
		textField.setString(text);
	}

	public void insert(String src, int pos) {
		textField.insert(src, pos);
	}

	public void insert(char[] data, int offset, int length, int position) {
		insert(new String(data, offset, length), position);
	}

	public String getString() {
		return textField.getString();
	}

	public int size() {
		return textField.size();
	}

	public int setMaxSize(int maxSize) {
		return textField.setMaxSize(maxSize);
	}

	public int getMaxSize() {
		return textField.getMaxSize();
	}

	public void setConstraints(int constraints) {
		textField.setConstraints(constraints);
	}

	public int getConstraints() {
		return textField.getConstraints();
	}

	public void setInitialInputMode(String characterSubset) {
	}

	public void setChars(char[] data, int offset, int length) {
		setString(new String(data, offset, length));
	}

	public int getChars(char[] data) {
		return textField.getChars(data);
	}

	public int getCaretPosition() {
		return textField.getCaretPosition();
	}

	public void delete(int offset, int length) {
		textField.delete(offset, length);
	}

	@Override
	public View getScreenView() {
		Context context = getParentActivity();
		return textField.getView(context, null);
	}

	@Override
	public void clearScreenView() {
		textField.clearScreenView();
	}
}
