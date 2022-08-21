/*
 *  Copyright 2019-2022 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.microedition.lcdui.commands;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;

import java.util.Arrays;
import java.util.List;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Screen;

import ru.playsoftware.j2meloader.R;

public class ScreenSoftBar extends AbstractSoftKeysBar {
	private final View layout;
	private final Button btLeft;
	private final Button btMiddle;
	private final Button btRight;

	public ScreenSoftBar(Screen target, View root) {
		super(target);
		layout = root.findViewById(R.id.softBar);
		btLeft = layout.findViewById(R.id.softLeft);
		btMiddle = layout.findViewById(R.id.softMiddle);
		btRight = layout.findViewById(R.id.softRight);
		btLeft.setOnClickListener(this::onClick);
		btMiddle.setOnClickListener(this::onClick);
		btRight.setOnClickListener(this::onClick);
		notifyChanged();
	}

	private void onClick(View button) {
		Object tag = button.getTag();
		if (tag == null) {
			PopupWindow popup = prepareMenu(2);
			int y = btRight.getHeight();
			View rootView = btRight.getRootView();
			popup.setWidth(Math.min(rootView.getWidth(), rootView.getHeight()) / 2);
			popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			popup.showAtLocation(rootView, Gravity.RIGHT | Gravity.BOTTOM, 0, y);
		} else {
			target.fireCommandAction((Command) tag);
		}
	}

	@Override
	protected void onCommandsChanged() {
		List<Command> list = this.commands;
		list.clear();
		Command[] commands = target.getCommands();
		int size = commands.length;
		if (size == 0) {
			btLeft.setTag(null);
			btMiddle.setTag(null);
			btRight.setTag(null);
			btLeft.setText("");
			btMiddle.setText("");
			btRight.setText("");
			layout.setVisibility(View.GONE);
			return;
		}
		Arrays.sort(commands);
		list.addAll(Arrays.asList(commands));
		switch (size) {
			case 1:
				Command c = list.get(0);
				btLeft.setText(c.getAndroidLabel());
				btLeft.setTag(c);

				btMiddle.setVisibility(View.INVISIBLE);
				btMiddle.setText("");
				btMiddle.setTag(null);

				btRight.setVisibility(View.INVISIBLE);
				btRight.setText("");
				btRight.setTag(null);
				break;
			case 2:
				c = list.get(0);
				btLeft.setText(c.getAndroidLabel());
				btLeft.setTag(c);

				btMiddle.setVisibility(View.INVISIBLE);
				btMiddle.setText("");
				btMiddle.setTag(null);

				btRight.setVisibility(View.VISIBLE);
				c = list.get(1);
				btRight.setText(c.getAndroidLabel());
				btRight.setTag(c);
				break;
			case 3:
				c = list.get(0);
				btLeft.setText(c.getAndroidLabel());
				btLeft.setTag(c);

				btMiddle.setVisibility(View.VISIBLE);
				c = list.get(1);
				btMiddle.setText(c.getAndroidLabel());
				btMiddle.setTag(c);

				btRight.setVisibility(View.VISIBLE);
				c = list.get(2);
				btRight.setText(c.getAndroidLabel());
				btRight.setTag(c);
				break;
			default:
				c = list.get(0);
				btLeft.setText(c.getAndroidLabel());
				btLeft.setTag(c);

				btMiddle.setVisibility(View.VISIBLE);
				c = list.get(1);
				btMiddle.setText(c.getAndroidLabel());
				btMiddle.setTag(c);

				btRight.setVisibility(View.VISIBLE);
				btRight.setText(R.string.cmd_menu);
				btRight.setTag(null);
		}
		layout.setVisibility(View.VISIBLE);
	}
}
