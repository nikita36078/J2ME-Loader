/*
 * Copyright 2018 Nikita Shakarun
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

import com.mascotcapsule.micro3d.v3.impl.ActionTableImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.util.ContextHolder;

public class ActionTable {
	private ActionTableImpl actionTable;

	public ActionTable(byte[] b) throws RuntimeException {
		if (b == null) {
			throw new NullPointerException();
		}
		try {
			actionTable = new ActionTableImpl(new ByteArrayInputStream(b));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Loading error");
			throw new RuntimeException(e);
		}
	}

	public ActionTable(String name) throws IOException {
		if (name == null) {
			throw new NullPointerException();
		}
		InputStream is = ContextHolder.getResourceAsStream(null, name);
		if (is == null) {
			throw new IOException();
		}
		try {
			actionTable = new ActionTableImpl(is);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Loading error");
			throw new RuntimeException(e);
		}
	}

	public final void dispose() {
	}

	public final int getNumAction() {
		return getNumActions();
	}

	public final int getNumActions() {
		return actionTable.getNumActions();
	}

	public final int getNumFrame(int idx) {
		return getNumFrames(idx);
	}

	public final int getNumFrames(int idx) {
		if (idx >= 0 && idx < getNumAction()) {
			return actionTable.getNumFrames(idx);
		} else {
			throw new IllegalArgumentException();
		}

	}
}