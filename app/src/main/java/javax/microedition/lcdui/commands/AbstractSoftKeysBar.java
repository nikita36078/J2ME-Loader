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
import java.util.Arrays;
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
	protected boolean middleSoft;
	protected Command middle;
	protected Command right;
	protected int menuStartIndex;

	protected AbstractSoftKeysBar(Displayable target, boolean middleSoft) {
		this.target = target;
		this.middleSoft = middleSoft;
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

	protected void onCommandsChanged() {
		commands.clear();
		Command[] arr = target.getCommands();
		Arrays.sort(arr);
		commands.addAll(Arrays.asList(arr));
		middle = null;
		right = null;
		for (Command cmd: arr) {
			int type = cmd.getCommandType();
			switch(type) {
				case Command.OK:
					if (middle != null) continue;
					middle = cmd;
					commands.remove(cmd);
					break;
				case Command.BACK:
				case Command.EXIT:
					if (right != null) continue;
					right = cmd;
					commands.remove(cmd);
					break;
			}
		}
		int i = 0;
		if (middle != null) {
			commands.add(0, middle);
			if (commands.size() == 1 || !middleSoft) {
				middle = null;
			} else {
				i++;
			}
		}
		if (right != null) {
			commands.add(0, right);
			i++;
		}
		menuStartIndex = i;
	}

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
