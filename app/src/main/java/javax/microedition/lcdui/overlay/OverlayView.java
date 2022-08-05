/*
 * Copyright 2019-2022 Yury Kharchenko
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
package javax.microedition.lcdui.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

import javax.microedition.lcdui.graphics.CanvasWrapper;

public class OverlayView extends View {

	private final CanvasWrapper graphics;
	private final ArrayList<Layer> layers = new ArrayList<>();
	private boolean visible;
	private int x;
	private int y;

	public OverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (isInEditMode()) { // fix for IDE preview
			graphics = null;
		} else {
			graphics = new CanvasWrapper(false);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (!visible) return;
		int save = canvas.save();
		canvas.translate(x, y);
		graphics.bind(canvas);
		for (Layer layer : layers) {
			layer.paint(graphics);
		}
		canvas.restoreToCount(save);
	}

	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void addLayer(Layer layer) {
		if (layers.contains(layer)) {
			return;
		}
		layers.add(layer);
	}

	public void addLayer(Layer layer, int order) {
		if (layers.contains(layer)) {
			return;
		}
		layers.add(Math.max(0, Math.min(order, layers.size())), layer);
	}

	public void removeLayer(Layer layer) {
		layers.remove(layer);
	}

	public void setVisibility(boolean visibility) {
		visible = visibility && !layers.isEmpty();
		postInvalidate();
	}
}
