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

import java.util.Enumeration;

public class MultiTexture extends Texture {
	public static final int MAX_TEXTURES = 16;

	public MultiTexture() {
	}

	public MultiTexture(int capacity) {
	}

	public boolean addTexture(Texture texture) {
		return false;
	}

	public int capacity() {
		return 0;
	}

	public boolean contains(Texture texture) {
		return false;
	}

	public int getCurrentIndex() {
		return 0;
	}

	public Texture getCurrentTexture() {
		return null;
	}

	public int indexOf(Texture texture) {
		return 0;
	}

	public int indexOf(Texture texture, int index) {
		return 0;
	}

	public boolean insertTextureAt(Texture texture, int index) {
		return false;
	}

	public boolean isEmpty() {
		return false;
	}

	public int lastIndexOf(Texture texture) {
		return 0;
	}

	public int lastIndexOf(Texture texture, int index) {
		return 0;
	}

	public void removeAllTextures() {
	}

	public boolean removeTexture(Texture texture) {
		return false;
	}

	public void removeTextureAt(int index) {
	}

	public void setCurrentIndex(int index) {
	}

	public void setTextureAt(Texture texture, int index) {
	}

	public int size() {
		return 0;
	}

	public Texture textureAt(int index) {
		return null;
	}

	public Enumeration textures() {
		return null;
	}
}
