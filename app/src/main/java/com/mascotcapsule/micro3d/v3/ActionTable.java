/*
 * Copyright 2020 Yury Kharchenko
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

package com.mascotcapsule.micro3d.v3;

import static com.mascotcapsule.micro3d.v3.Util3D.TAG;

import android.util.Log;

import java.io.IOException;

import javax.microedition.shell.AppClassLoader;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ActionTable {
	Action[] actions;

	public ActionTable(byte[] b) {
		if (b == null) {
			throw new NullPointerException();
		}
		try {
			actions = Loader.loadMtraData(b);
		} catch (IOException e) {
			Log.e(TAG, "Error loading data", e);
			throw new RuntimeException(e);
		}
	}

	public ActionTable(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		byte[] bytes = AppClassLoader.getResourceAsBytes(name);
		if (bytes == null) {
			throw new IOException();
		}
		try {
			actions = Loader.loadMtraData(bytes);
		} catch (IOException e) {
			Log.e(TAG, "Error loading data from [" + name + "]", e);
			throw new RuntimeException(e);
		}
	}

	public final void dispose() {
		actions = null;
	}

	public final int getNumAction() {
		return getNumActions();
	}

	public final int getNumActions() {
		checkDisposed();
		return actions.length;
	}

	public final int getNumFrame(int idx) {
		return getNumFrames(idx);
	}

	public final int getNumFrames(int idx) {
		checkDisposed();
		if (idx < 0 || idx >= actions.length) {
			throw new IllegalArgumentException();
		}
		return actions[idx].keyframes << 16;
	}

	void checkDisposed() {
		if (actions == null) throw new IllegalStateException("ActionTable disposed!");
	}

}
