/*
 *  Copyright 2022 Yury Kharchenko
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

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.util.ContextHolder;

public abstract class AbstractSoftKeysBar {
	protected final Displayable target;
	protected final List<Command> commands = new ArrayList<>();
	private PopupWindow popup;
	private ArrayAdapter<Command> adapter;

	protected AbstractSoftKeysBar(Displayable target) {
		this.target = target;
	}

	public void notifyChanged() {
		ViewHandler.postEvent(this::onCommandsChanged);
	}

	protected PopupWindow prepareMenu(int skip) {
		if (popup == null) {
			Context context = ContextHolder.getActivity();
			popup = new PopupWindow(context, null, androidx.appcompat.R.attr.actionOverflowMenuStyle);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				popup.setExitTransition(null);
			}
			popup.setOutsideTouchable(true);
			popup.setFocusable(true);
			ListView lv = new ListView(context);
			popup.setContentView(lv);
			adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
			adapter.setNotifyOnChange(true);
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(this::onMenuItemClick);
			popup.setOnDismissListener(() -> adapter.clear());
		}
		adapter.addAll(skip == 0 ? commands : commands.subList(skip, commands.size()));
		return popup;
	}

	protected abstract void onCommandsChanged();

	public void closeMenu() {
		if (popup != null && popup.isShowing()) {
			popup.dismiss();
		}
	}

	private void onMenuItemClick(AdapterView<?> parent, View view, int position, long id) {
		Command cmd = (Command) parent.getItemAtPosition(position);
		target.fireCommandAction(cmd);
		popup.dismiss();
	}
}
