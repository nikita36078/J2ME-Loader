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

import androidx.appcompat.widget.AppCompatImageView;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class ImageItem extends Item {
	private Image image;
	private ImageView imageView;
	private String altText;
	private final int appearanceMode;

	private final SimpleEvent msgUpdateImageView = new SimpleEvent() {
		@Override
		public void process() {
			updateImageView();
		}
	};

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

		if (imageView != null) {
			ViewHandler.postEvent(msgUpdateImageView);
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

	private void updateImageView() {
		if (image != null) {
			int virtualWidth = Displayable.getVirtualWidth();
			int displayWidth = ContextHolder.getDisplayWidth();
			float mul = (float) displayWidth / virtualWidth;
			int width = (int) (image.getWidth() * mul);
			int height = (int) (image.getHeight() * mul);
			imageView.setMinimumWidth(width);
			imageView.setMinimumHeight(height);
			imageView.setImageBitmap(image.getBitmap());
		} else {
			imageView.setImageBitmap(null);
		}
	}

	@Override
	public View getItemContentView() {
		if (imageView == null) {
			imageView = new AppCompatImageView(ContextHolder.getActivity());
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setOnClickListener(v -> fireDefaultCommandAction());
			updateImageView();
		}

		return imageView;
	}

	@Override
	public void clearItemContentView() {
		imageView = null;
	}
}