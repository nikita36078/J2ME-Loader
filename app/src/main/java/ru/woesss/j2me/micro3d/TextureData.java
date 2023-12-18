/*
 * Copyright 2022 Yury Kharchenko
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

package ru.woesss.j2me.micro3d;

import java.nio.ByteBuffer;

class TextureData {
	private final ByteBuffer raster;
	final int width;
	final int height;

	TextureData(int width, int height) {
		this.raster = BufferUtils.createByteBuffer(width * height * 4);
		this.width = width;
		this.height = height;
	}

	ByteBuffer getRaster() {
		raster.rewind();
		return raster;
	}
}
