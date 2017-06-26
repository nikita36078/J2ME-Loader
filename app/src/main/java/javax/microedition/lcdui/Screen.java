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

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.microedition.lcdui.event.SimpleEvent;

public abstract class Screen extends Displayable {
	private static final int TICKER_NO_ACTION = 0;
	private static final int TICKER_SHOW = 1;
	private static final int TICKER_HIDE = 2;

	private Ticker ticker;

	private LinearLayout layout;
	private TextView marquee;
	private int tickermode;

	private SimpleEvent msgSetTicker = new SimpleEvent() {
		public void process() {
			if (ticker != null) {
				marquee.setText(ticker.getString());
			}

			switch (tickermode) {
				case TICKER_SHOW:
					layout.addView(marquee, 0);
					break;

				case TICKER_HIDE:
					layout.removeView(marquee);
					break;
			}

			tickermode = TICKER_NO_ACTION;
		}
	};

	public void setTicker(Ticker newticker) {
		if (layout != null) {
			if (ticker == null && newticker != null) {
				tickermode = TICKER_SHOW;
			} else if (ticker != null && newticker == null) {
				tickermode = TICKER_HIDE;
			}

			ticker = newticker;

			ViewHandler.postEvent(msgSetTicker);
		} else {
			ticker = newticker;
		}
	}

	public Ticker getTicker() {
		return ticker;
	}

	public View getDisplayableView() {
		if (layout == null) {
			Context context = getParentActivity();

			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);

			marquee = new TextView(context);
			marquee.setTextAppearance(context, android.R.style.TextAppearance_Medium);
			marquee.setEllipsize(TruncateAt.MARQUEE);
			marquee.setSelected(true);

			if (ticker != null) {
				marquee.setText(ticker.getString());
				layout.addView(marquee);
			}

			layout.addView(getScreenView());
		}

		return layout;
	}

	public void clearDisplayableView() {
		layout = null;
		marquee = null;

		clearScreenView();
	}

	public abstract View getScreenView();

	public abstract void clearScreenView();
}
