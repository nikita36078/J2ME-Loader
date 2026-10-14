/*
 * Copyright 2026 Nikita Shakarun
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

package javax.microedition.content;

public final class ActionNameMap {
	private final String[] actions;
	private final String[] actionNames;
	private final String locale;

	public ActionNameMap(String[] actions, String[] actionnames, String locale) {
		this.actions = actions;
		this.actionNames = actionnames;
		this.locale = locale;
	}

	public String getActionName(String action) {
		for (int i = 0; i < actions.length; i++) {
			if (actions[i].equals(action)) return actionNames[i];
		}
		return null;
	}

	public String getAction(String actionname) {
		for (int i = 0; i < actionNames.length; i++) {
			if (actionNames[i].equals(actionname)) return actions[i];
		}
		return null;
	}

	public String getLocale() {
		return locale;
	}

	public int size() {
		return actions.length;
	}

	public String getAction(int index) {
		return actions[index];
	}

	public String getActionName(int index) {
		return actionNames[index];
	}
}
