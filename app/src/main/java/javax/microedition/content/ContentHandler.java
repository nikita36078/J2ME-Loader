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

public interface ContentHandler {
	String ACTION_OPEN = "open";
	String ACTION_EDIT = "edit";
	String ACTION_SEND = "send";
	String ACTION_SAVE = "save";
	String ACTION_EXECUTE = "execute";
	String ACTION_SELECT = "select";
	String ACTION_INSTALL = "install";
	String ACTION_PRINT = "print";
	String ACTION_NEW = "new";
	String ACTION_STOP = "stop";
	String UNIVERSAL_TYPE = "*";

	String getID();

	String getType(int index);

	int getTypeCount();

	boolean hasType(String type);

	String getSuffix(int index);

	int getSuffixCount();

	boolean hasSuffix(String suffix);

	String getAction(int index);

	int getActionCount();

	boolean hasAction(String action);

	ActionNameMap getActionNameMap();

	ActionNameMap getActionNameMap(String locale);

	int getActionNameMapCount();

	ActionNameMap getActionNameMap(int index);

	String getAppName();

	String getVersion();

	String getAuthority();
}
