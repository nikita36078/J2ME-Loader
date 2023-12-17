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

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Screen;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.SoftButtonBarBinding;

public class ScreenSoftBar extends AbstractSoftKeysBar {
	private final SoftButtonBarBinding binding;

	public ScreenSoftBar(Screen target, SoftButtonBarBinding binding) {
		super(target, true);
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
			PopupWindow popup = prepareMenu(menuStartIndex);
			int y = binding.rightButton.getHeight();
			View rootView = binding.rightButton.getRootView();
			popup.setWidth(Math.min(rootView.getWidth(), rootView.getHeight()) / 2);
			popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			popup.showAtLocation(rootView, Gravity.LEFT | Gravity.BOTTOM, 0, y);
		} else {
			target.fireCommandAction((Command) tag);
		}
	}

	@Override
	protected void onCommandsChanged() {
		binding.leftButton.setTag(null);
		binding.middleButton.setTag(null);
		binding.rightButton.setTag(null);
		binding.leftButton.setText("");
		binding.middleButton.setText("");
		binding.rightButton.setText("");
		binding.leftButton.setVisibility(View.INVISIBLE);
		binding.middleButton.setVisibility(View.INVISIBLE);
		binding.rightButton.setVisibility(View.INVISIBLE);

		super.onCommandsChanged();
		int size = commands.size();
		if (size == 0) {
			binding.rootLayout.setVisibility(View.GONE);
			return;
		}
		if (size - menuStartIndex > 1) {
			binding.leftButton.setVisibility(View.VISIBLE);
			binding.leftButton.setText(R.string.cmd_menu);
		} else if (menuStartIndex < size) {
			Command left = commands.get(menuStartIndex);
			setCommand(binding.leftButton, left);
		}
		if (right != null) {
			setCommand(binding.rightButton, right);
			if (middle != null) {
				setCommand(binding.middleButton, middle);
			}
		} else {
			if (middle != null) {
				setCommand(binding.rightButton, middle);
			}
		}
		binding.rootLayout.setVisibility(View.VISIBLE);
	}

	private void setCommand(Button binding, Command c) {
		binding.setVisibility(View.VISIBLE);
		binding.setText(c.getAndroidLabel());
		binding.setTag(c);
	}
}
