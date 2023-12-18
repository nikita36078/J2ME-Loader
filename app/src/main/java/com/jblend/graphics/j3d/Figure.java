/*
 * Copyright 2022-2023 Yury Kharchenko
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

package com.jblend.graphics.j3d;

import java.io.IOException;

import ru.woesss.j2me.micro3d.FigureImpl;

public class Figure {
	Texture[] textures;
	FigureImpl impl;

	
	public Figure(byte[] b) {
		impl = new FigureImpl(b);
	}

	
	public Figure(String name) throws IOException {
		impl = new FigureImpl(name);
	}

	
	/** @noinspection unused*/
	public int getNumPattern() {
		return impl.getNumPattern();
	}

	
	/** @noinspection unused*/
	public int getNumTextures() {
		if (textures == null) {
			return 0;
		}
		return textures.length;
	}

	
	public void setPattern(int idx) {
		impl.setPattern(idx);
	}

	
	/** @noinspection unused*/
	public void setPosture(ActionTable actionTable, int action, int frame) {
		if (actionTable == null) {
			throw new NullPointerException();
		}
		impl.setPosture(actionTable.impl, action, frame);
	}

	public void setTexture(Texture tex) {
		if (tex == null)
			throw new NullPointerException();
		if (!tex.isForModel)
			throw new IllegalArgumentException();

		textures = new Texture[]{tex};
	}

	public void setTexture(Texture[] t) {
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
	}
}
