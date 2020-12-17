/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
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

package javax.microedition.lcdui.list;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;

public class CompoundItem {
	private final String text;
	private final Image icon;
	private Drawable imageDrawable;
	private boolean selected = false;
	private Font mFont = Font.getDefaultFont();

	public CompoundItem(String stringPart) {
		this(stringPart, null);
	}

	public CompoundItem(String stringPart, Image imagePart) {
		this.text = stringPart;
		this.icon = imagePart;
	}

	public String getString() {
		return text;
	}

	public Image getImage() {
		return icon;
	}

	public Drawable getDrawable(float height) {
		if (imageDrawable == null && icon != null) {
			Bitmap bitmap = icon.getBitmap();
			int width = Math.round(bitmap.getWidth() * height / bitmap.getHeight());
			imageDrawable = new BitmapDrawable(bitmap);
			imageDrawable.setBounds(0, 0, width, Math.round(height));
		}
		return imageDrawable;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public Font getFont() {
		return mFont;
	}

	public void setFont(Font font) {
		if (font == null) {
			font = Font.getDefaultFont();
		}
		mFont = font;
	}
}