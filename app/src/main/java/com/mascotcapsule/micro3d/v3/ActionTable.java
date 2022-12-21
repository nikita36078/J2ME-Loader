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

import java.io.IOException;

import ru.woesss.j2me.micro3d.ActTableImpl;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ActionTable {
	final ActTableImpl impl;

	public ActionTable(byte[] b) {
		impl = new ActTableImpl(b);
	}

	public ActionTable(String name) throws IOException {
		impl = new ActTableImpl(name);
	}

	public final void dispose() {
		impl.dispose();
	}

	@Deprecated
	public final int getNumAction() {
		return impl.getNumActions();
	}

	public final int getNumActions() {
		return impl.getNumActions();
	}

	@Deprecated
	public final int getNumFrame(int idx) {
		return impl.getNumFrames(idx);
	}

	public final int getNumFrames(int idx) {
		return impl.getNumFrames(idx);
	}
}
