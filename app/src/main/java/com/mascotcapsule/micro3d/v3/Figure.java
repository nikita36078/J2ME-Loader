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

import ru.woesss.j2me.micro3d.FigureImpl;

import java.io.IOException;

public class Figure {
	final FigureImpl impl;

	@SuppressWarnings("unused")
	public Figure(byte[] b) {
		impl = new FigureImpl(b);
	}

	@SuppressWarnings("unused")
	public Figure(String name) throws IOException {
		impl = new FigureImpl(name);
	}

	@SuppressWarnings("unused")
	public final void dispose() {
		impl.dispose();
	}

	@SuppressWarnings("unused")
	public final void setPosture(ActionTable actionTable, int action, int frame) {
		if (actionTable == null) {
			throw new NullPointerException();
		} else if (action < 0 || action >= actionTable.getNumActions()) {
			throw new IllegalArgumentException();
		}
		impl.setPosture(actionTable.impl, action, frame);
	}

	public final Texture getTexture() {
		return impl.getTexture();
	}

	public final void setTexture(Texture tex) {
		impl.setTexture(tex);
	}

	public final void setTexture(Texture[] t) {
		impl.setTexture(t);
	}

	@SuppressWarnings("unused")
	public final int getNumTextures() {
		return impl.getNumTextures();
	}

	@SuppressWarnings("unused")
	public final void selectTexture(int idx) {
		impl.selectTexture(idx);
	}

	@SuppressWarnings("unused")
	public final int getNumPattern() {
		return impl.getNumPattern();
	}

	@SuppressWarnings("unused")
	public final void setPattern(int idx) {
		impl.setPattern(idx);
	}
}
