/*
 * Copyright 2012 Kulikov Dmitriy
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

import android.view.View;
import android.widget.ImageView;

public class ImageItem extends Item {
	private Image image;
	private ImageView imgview;
	private String altText;
	private int appearanceMode;

	public ImageItem(String label, Image image, int layout, String altText) {
		this(label, image, layout, altText, PLAIN);
	}

	public ImageItem(String label, Image image, int layout, String altText, int appearanceMode) {
		setLabel(label);
		setImage(image);
		setLayout(layout);
		setAltText(altText);
		this.appearanceMode = appearanceMode;
	}

	public void setImage(Image img) {
		image = img;

		if (imgview != null) {
			imgview.setImageBitmap(image != null ? image.getBitmap() : null);
		}
	}

	public Image getImage() {
		return image;
	}

	public String getAltText() {
		return altText;
	}

	public void setAltText(String text) {
		altText = text;
	}

	public int getAppearanceMode() {
		return appearanceMode;
	}

	@Override
	public View getItemContentView() {
		if (imgview == null) {
			imgview = new ImageView(getOwnerForm().getParentActivity());
			imgview.setImageBitmap(image != null ? image.getBitmap() : null);
			imgview.setOnClickListener(v -> fireDefaultCommandAction());
		}

		return imgview;
	}

	@Override
	public void clearItemContentView() {
		imgview = null;
	}
}