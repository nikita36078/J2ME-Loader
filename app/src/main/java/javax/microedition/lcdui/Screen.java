/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
 * Copyright 2022 Yury Kharchenko
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import javax.microedition.lcdui.commands.ScreenSoftBar;

import ru.playsoftware.j2meloader.databinding.SoftButtonBarBinding;

public abstract class Screen extends Displayable {

	private LinearLayout layout;

	@Override
	public View getDisplayableView() {
		if (layout == null) {
			layout = (LinearLayout) super.getDisplayableView();

			View screenView = getScreenView();
			screenView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
			layout.addView(screenView);

			SoftButtonBarBinding binding = SoftButtonBarBinding.inflate(
					LayoutInflater.from(layout.getContext()), layout, true);
			softBar = new ScreenSoftBar(this, binding);
		}

		return layout;
	}

	@Override
	public void clearDisplayableView() {
		super.clearDisplayableView();
		layout = null;
		softBar.closeMenu();
		softBar = null;
		clearScreenView();
	}

	public abstract View getScreenView();

	public abstract void clearScreenView();
}
