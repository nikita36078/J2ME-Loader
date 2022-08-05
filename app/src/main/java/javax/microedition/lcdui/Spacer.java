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
import android.widget.Space;

import javax.microedition.util.ContextHolder;

public class Spacer extends Item {
	private int width, height;
	private View view;

	public Spacer(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setMinimumSize(int width, int height) {
		this.width = width;
		this.height = height;

		if (view != null) {
			view.setMinimumWidth(width);
			view.setMinimumHeight(height);
		}
	}

	@Override
	public View getItemContentView() {
		if (view == null) {
			view = new Space(ContextHolder.getActivity());

			view.setMinimumWidth(width);
			view.setMinimumHeight(height);
		}

		return view;
	}

	@Override
	public void clearItemContentView() {
		view = null;
	}
}