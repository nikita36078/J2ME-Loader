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

import ru.woesss.j2me.micro3d.FigureImpl;

public class Figure {
	Texture[] textures;
	int textureIndex = -1;
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
	public final int getNumPattern() {
		return impl.getNumPattern();
	}

	@SuppressWarnings("unused")
	public final int getNumTextures() {
		if (textures == null) {
			return 0;
		}
		return textures.length;
	}

	public final Texture getTexture() {
		if (textureIndex < 0) {
			return null;
		}
		return textures[textureIndex];
	}

	@SuppressWarnings("unused")
	public final void selectTexture(int idx) {
		if (idx < 0 || idx >= getNumTextures()) {
			throw new IllegalArgumentException();
		}
		textureIndex = idx;
	}

	@SuppressWarnings("unused")
	public final void setPattern(int idx) {
		impl.setPattern(idx);
	}

	@SuppressWarnings("unused")
	public final void setPosture(ActionTable actionTable, int action, int frame) {
		if (actionTable == null) {
			throw new NullPointerException();
		}
		impl.setPosture(actionTable.impl, action, frame);
	}

	public final void setTexture(Texture tex) {
		if (tex == null)
			throw new NullPointerException();
		if (!tex.isForModel)
			throw new IllegalArgumentException();

		textures = new Texture[]{tex};
		textureIndex = 0;
	}

	public final void setTexture(Texture[] t) {
		if (t == null) {
			throw new NullPointerException();
		}
		if (t.length == 0) {
			throw new IllegalArgumentException();
		}
		for (Texture texture : t) {
			if (texture == null) {
				throw new NullPointerException();
			}
			if (!texture.isForModel) {
				throw new IllegalArgumentException();
			}
		}
		textures = t;
		textureIndex = -1;
	}
}
