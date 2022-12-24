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

package com.motorola.iden.micro3d;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public class MultiTexture extends Texture {
	public static final int MAX_TEXTURES = 16;
	private int currentIdx = 0;

	Texture[] textures;
	int size;
	private Texture currentTexture;

	public MultiTexture() {
		super(false);
		textures = new Texture[16];
	}

	public MultiTexture(int capacity) {
		super(false);
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		textures = new Texture[Math.min(capacity, MAX_TEXTURES)];
	}

	public boolean addTexture(Texture texture) {
		if (texture == null) {
			throw new NullPointerException();
		}
		if (texture.isSphereTexture() || texture instanceof MultiTexture) {
			throw new IllegalArgumentException();
		}

		if (size < MAX_TEXTURES) {
			Texture[] copy;
			if (size < textures.length) {
				copy = textures;
			} else {
				copy = new Texture[size + 1];
				System.arraycopy(textures, 0, copy, 0, size);
			}
			textures[size++] = texture;
			textures = copy;
			return true;
		}
		return false;
	}

	public int capacity() {
		return textures.length;
	}

	public boolean contains(Texture texture) {
		for (Texture t : textures) {
			if (t == texture) {
				return true;
			}
		}
		return false;
	}

	public int getCurrentIndex() {
		return currentIdx;
	}

	public Texture getCurrentTexture() {
		return currentTexture;
	}

	public int indexOf(Texture texture) {
		return indexOf(texture, 0);
	}

	public int indexOf(Texture texture, int index) {
		for (int len = textures.length; index < len; index++) {
			if (textures[index] == texture) {
				return index;
			}
		}
		return -1;
	}

	public boolean insertTextureAt(Texture texture, int index) {
		if (texture == null) {
			throw new NullPointerException();
		}
		if (texture.isSphereTexture() || texture instanceof MultiTexture) {
			throw new IllegalArgumentException();
		}
		if (index < 0 || index > size) {
			throw new ArrayIndexOutOfBoundsException();
		}

		if (size < MAX_TEXTURES) {
			Texture[] copy;
			if (size < textures.length) {
				copy = textures;
			} else {
				copy = new Texture[size + 1];
				System.arraycopy(textures, 0, copy, 0, index);
			}
			System.arraycopy(textures, index, copy, index + 1, size - index);
			textures[index] = texture;
			textures = copy;
			size++;
			return true;
		}
		return false;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int lastIndexOf(Texture texture) {
		return lastIndexOf(texture, textures.length);
	}

	public int lastIndexOf(Texture texture, int index) {
		for (index--; index >= 0; index--) {
			if (textures[index] == texture) {
				return index;
			}
		}
		return -1;
	}

	public void removeAllTextures() {
		while (size > 0) {
			size--;
			textures[size] = null;
		}
	}

	public boolean removeTexture(Texture texture) {
		int index = indexOf(texture);
		if (index == -1) {
			return false;
		}
		removeTextureAt(index);
		return true;
	}

	public void removeTextureAt(int index) {
		if (index < 0 || index >= size) {
			throw new ArrayIndexOutOfBoundsException();
		}
		size--;
		System.arraycopy(textures, index + 1, textures, index, size - index);
	}

	public void setCurrentIndex(int index) {
		if (index < 0 || index >= size) {
			throw new ArrayIndexOutOfBoundsException();
		}
		currentIdx = index;
		currentTexture = textures[index];
	}

	public void setTextureAt(Texture texture, int index) {
		if (texture == null) {
			throw new NullPointerException();
		}
		if (texture.isSphereTexture() || texture instanceof MultiTexture) {
			throw new IllegalArgumentException();
		}
		if (index < 0 || index >= size) {
			throw new ArrayIndexOutOfBoundsException();
		}
		textures[index] = texture;
	}

	public int size() {
		return size;
	}

	public Texture textureAt(int index) {
		if (index < 0 || index > size) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return textures[index];
	}

	public Enumeration textures() {
		return Collections.enumeration(Arrays.asList(textures).subList(0, size));
	}
}
