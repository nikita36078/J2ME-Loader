/*
 * Copyright 2019 Yury Kharchenko
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

import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.graphics.CanvasWrapper;

public class FpsCounter extends TimerTask implements Layer {

	private final View view;
	private String prevFrameCount = "0";
	private int totalFrameCount;
	private final Timer timer;

	public FpsCounter(View view) {
		this.view = view;
		timer = new Timer("FpsCounter", true);
		timer.scheduleAtFixedRate(this, 0, 1000);
	}

	public void run() {
		prevFrameCount = String.valueOf(totalFrameCount);
		totalFrameCount = 0;
		view.postInvalidate();
	}

	public void increment() {
		totalFrameCount++;
	}

	public void paint(CanvasWrapper g) {
		g.setFillColor(0x90000000);
		g.setTextColor(0xFF00FF00);
		g.drawBackgroundedText(prevFrameCount);
	}

	public void stop() {
		timer.cancel();
	}
}
