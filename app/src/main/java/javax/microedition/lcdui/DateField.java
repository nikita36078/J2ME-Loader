/*
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

package javax.microedition.lcdui;

import android.content.Context;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class DateField extends Item {
	public static final int DATE = 1;
	public static final int DATE_TIME = 3;
	public static final int TIME = 2;

	private int mode;
	private Calendar calendar = Calendar.getInstance();

	private LinearLayout layout;
	private DatePicker datePicker;
	private TimePicker timePicker;

	private SimpleEvent msgUpdateDate = new SimpleEvent() {
		@Override
		public void process() {
			datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH));
			timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
			timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
		}
	};

	private SimpleEvent msgSetVisibility = new SimpleEvent() {
		@Override
		public void process() {
			if (mode == DATE) {
				datePicker.setVisibility(View.VISIBLE);
				timePicker.setVisibility(View.GONE);
			} else if (mode == TIME) {
				datePicker.setVisibility(View.GONE);
				timePicker.setVisibility(View.VISIBLE);
			} else if (mode == DATE_TIME) {
				datePicker.setVisibility(View.VISIBLE);
				timePicker.setVisibility(View.VISIBLE);
			}
		}
	};

	private class DateChangedListener implements DatePicker.OnDateChangedListener {
		@Override
		public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			calendar.set(year, monthOfYear, dayOfMonth);
		}
	}

	private class TimeChangedListener implements TimePicker.OnTimeChangedListener {
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
			calendar.set(Calendar.MINUTE, minute);
		}
	}

	private DateChangedListener dateChangedListener = new DateChangedListener();
	private TimeChangedListener timeChangedListener = new TimeChangedListener();

	public DateField(String label, int mode) {
		this(label, mode, TimeZone.getDefault());
	}

	public DateField(String label, int mode, TimeZone timeZone) {
		setDate(Calendar.getInstance(timeZone).getTime());
		setInputMode(mode);
		setLabel(label);
	}

	public Date getDate() {
		return calendar.getTime();
	}

	public int getInputMode() {
		return mode;
	}

	public void setDate(Date date) {
		calendar.setTime(date);
		if (layout != null) {
			ViewHandler.postEvent(msgUpdateDate);
		}
	}

	public void setInputMode(int mode) {
		this.mode = mode;
		if (layout != null) {
			ViewHandler.postEvent(msgSetVisibility);
		}
	}

	@Override
	protected View getItemContentView() {
		if (layout == null) {
			Context context = ContextHolder.getActivity();

			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);

			datePicker = new DatePicker(context);
			timePicker = new TimePicker(context);

			datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH), dateChangedListener);
			timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
			timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
			timePicker.setOnTimeChangedListener(timeChangedListener);

			layout.addView(datePicker);
			layout.addView(timePicker);

			if (mode == DATE) {
				datePicker.setVisibility(View.VISIBLE);
				timePicker.setVisibility(View.GONE);
			} else if (mode == TIME) {
				datePicker.setVisibility(View.GONE);
				timePicker.setVisibility(View.VISIBLE);
			} else if (mode == DATE_TIME) {
				datePicker.setVisibility(View.VISIBLE);
				timePicker.setVisibility(View.VISIBLE);
			}
		}

		return layout;
	}

	@Override
	protected void clearItemContentView() {
		layout = null;
	}
}
