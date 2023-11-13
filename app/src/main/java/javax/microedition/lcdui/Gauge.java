/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2021-2023 Yury Kharchenko
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
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class Gauge extends Item {
	public static final int CONTINUOUS_IDLE = 0;
	public static final int INCREMENTAL_IDLE = 1;
	public static final int CONTINUOUS_RUNNING = 2;
	public static final int INCREMENTAL_UPDATING = 3;
	public static final int INDEFINITE = -1;

	private final boolean interactive;
	private final SeekBarListener seekBarListener = new SeekBarListener();

	ProgressBar progressBar;
	private int value;
	private int maxValue;
	private Alert alert;

	public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
		setLabel(label);

		this.interactive = interactive;
		setMaxValue(maxValue);
		setValue(initialValue);
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
		if (this.maxValue == INDEFINITE && !interactive) {
			return;
		}
		ViewHandler.postEvent(new SimpleEvent() {
			@Override
			public void process() {
				ProgressBar progressBar = Gauge.this.progressBar;
				if (progressBar != null) {
					progressBar.setProgress(value);
				}
			}
		});
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
		ViewHandler.postEvent(new SimpleEvent() {
			@Override
			public void process() {
				ProgressBar progressBar = Gauge.this.progressBar;
				if (progressBar == null) {
					return;
				}
				if (maxValue != INDEFINITE || interactive) {
					progressBar.setIndeterminate(false);
					progressBar.setMax(maxValue);
				} else {
					progressBar.setIndeterminate(true);
				}
			}
		});
	}

	public boolean isInteractive() {
		return interactive;
	}

	@Override
	protected View getItemContentView() {
		if (progressBar == null) {
			Context activity = ContextHolder.getActivity();
			if (interactive) {
				progressBar = new AppCompatSeekBar(activity);
				((SeekBar) progressBar).setOnSeekBarChangeListener(seekBarListener);
			} else {
				progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
				progressBar.setIndeterminate(maxValue == INDEFINITE);
			}

			progressBar.setMax(maxValue);
			progressBar.setProgress(value);

			if (alert != null) {
				progressBar.setPadding(60, 0, 60, 0);
			}
		}

		return progressBar;
	}

	@Override
	protected void clearItemContentView() {
		progressBar = null;
	}

	void setAlert(Alert alert) {
		this.alert = alert;
	}

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
}
