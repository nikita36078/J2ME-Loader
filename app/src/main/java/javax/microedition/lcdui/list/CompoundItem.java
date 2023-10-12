/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
 * Copyright 2019-2023 Yury Kharchenko
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
	private String stringPart;
	private Image imagePart;
	private Drawable imageDrawable;
	private boolean selected;
	private Font mFont = Font.getDefaultFont();

	public CompoundItem(String stringPart, Image imagePart) {
		this(stringPart, imagePart, false);
	}

	public CompoundItem(String stringPart, Image imagePart, boolean selected) {
		this.stringPart = stringPart;
		this.imagePart = imagePart;
		this.selected = selected;
	}

	public String getString() {
		return stringPart;
	}

	public Image getImage() {
		return imagePart;
	}

	public Drawable getDrawable(float height) {
		if (imageDrawable == null && imagePart != null) {
			Bitmap bitmap = imagePart.getBitmap();
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

	public void set(String stringPart, Image imagePart) {
		this.stringPart = stringPart;
		this.imagePart = imagePart;
		imageDrawable = null;
	}
}
