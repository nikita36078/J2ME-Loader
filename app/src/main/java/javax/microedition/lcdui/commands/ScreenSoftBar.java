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
import android.widget.PopupWindow;

import java.util.Arrays;
import java.util.List;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Screen;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.SoftButtonBarBinding;

public class ScreenSoftBar extends AbstractSoftKeysBar {
	private final SoftButtonBarBinding binding;

	public ScreenSoftBar(Screen target, SoftButtonBarBinding binding) {
		super(target);
		// todo высвобождать глобальный binding после использования
		this.binding = binding;
		this.binding.leftButton.setOnClickListener(this::onClick);
		this.binding.middleButton.setOnClickListener(this::onClick);
		this.binding.rightButton.setOnClickListener(this::onClick);
		notifyChanged();
	}

	private void onClick(View button) {
		Object tag = button.getTag();
		if (tag == null) {
			PopupWindow popup = prepareMenu(2);
			int y = binding.rightButton.getHeight();
			View rootView = binding.rightButton.getRootView();
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
			binding.leftButton.setTag(null);
			binding.middleButton.setTag(null);
			binding.rightButton.setTag(null);
			binding.leftButton.setText("");
			binding.middleButton.setText("");
			binding.rightButton.setText("");
			binding.rootLayout.setVisibility(View.GONE);
			return;
		}
		Arrays.sort(commands);
		list.addAll(Arrays.asList(commands));
		switch (size) {
			case 1:
				Command c = list.get(0);
				binding.leftButton.setText(c.getAndroidLabel());
				binding.leftButton.setTag(c);

				binding.middleButton.setVisibility(View.INVISIBLE);
				binding.middleButton.setText("");
				binding.middleButton.setTag(null);

				binding.rightButton.setVisibility(View.INVISIBLE);
				binding.rightButton.setText("");
				binding.rightButton.setTag(null);
				break;
			case 2:
				c = list.get(0);
				binding.leftButton.setText(c.getAndroidLabel());
				binding.leftButton.setTag(c);

				binding.middleButton.setVisibility(View.INVISIBLE);
				binding.middleButton.setText("");
				binding.middleButton.setTag(null);

				binding.rightButton.setVisibility(View.VISIBLE);
				c = list.get(1);
				binding.rightButton.setText(c.getAndroidLabel());
				binding.rightButton.setTag(c);
				break;
			case 3:
				c = list.get(0);
				binding.leftButton.setText(c.getAndroidLabel());
				binding.leftButton.setTag(c);

				binding.middleButton.setVisibility(View.VISIBLE);
				c = list.get(1);
				binding.middleButton.setText(c.getAndroidLabel());
				binding.middleButton.setTag(c);

				binding.rightButton.setVisibility(View.VISIBLE);
				c = list.get(2);
				binding.rightButton.setText(c.getAndroidLabel());
				binding.rightButton.setTag(c);
				break;
			default:
				c = list.get(0);
				binding.leftButton.setText(c.getAndroidLabel());
				binding.leftButton.setTag(c);

				binding.middleButton.setVisibility(View.VISIBLE);
				c = list.get(1);
				binding.middleButton.setText(c.getAndroidLabel());
				binding.middleButton.setTag(c);

				binding.rightButton.setVisibility(View.VISIBLE);
				binding.rightButton.setText(R.string.cmd_menu);
				binding.rightButton.setTag(null);
		}
		binding.rootLayout.setVisibility(View.VISIBLE);
	}
}
