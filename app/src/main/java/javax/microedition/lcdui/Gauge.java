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
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

public class Gauge extends Item {
	public static final int CONTINUOUS_IDLE = 0;
	public static final int INCREMENTAL_IDLE = 1;
	public static final int CONTINUOUS_RUNNING = 2;
	public static final int INCREMENTAL_UPDATING = 3;

	public static final int INDEFINITE = -1;

	private ProgressBar pbar;

	private final boolean interactive;
	private int value, maxValue;

	private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser) {
				value = progress;
				notifyStateChanged();
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	}

	private final SeekBarListener listener = new SeekBarListener();

	public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
		setLabel(label);

		this.interactive = interactive;
		this.maxValue = maxValue;
		this.value = initialValue;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;

		if (pbar != null) {
			pbar.setProgress(value);
		}
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;

		if (pbar != null) {
			pbar.setMax(maxValue);
		}
	}

	@Override
	protected View getItemContentView() {
		if (pbar == null) {
			if (interactive) {
				pbar = new AppCompatSeekBar(getOwnerForm().getParentActivity());
				((SeekBar) pbar).setOnSeekBarChangeListener(listener);
			} else {
				pbar = new ProgressBar(getOwnerForm().getParentActivity(), null, android.R.attr.progressBarStyleHorizontal);
				pbar.setIndeterminate(maxValue == INDEFINITE);
			}

			pbar.setMax(maxValue);
			pbar.setProgress(value);
		}

		return pbar;
	}

	@Override
	protected void clearItemContentView() {
		pbar = null;
	}
}