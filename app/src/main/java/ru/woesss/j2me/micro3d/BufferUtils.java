/*
 * Copyright 2023 Yury Kharchenko
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
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BufferUtils {
	private BufferUtils(){}

	public static IntBuffer createIntBuffer(int capacity) {
		return createByteBuffer(capacity * 4).asIntBuffer();
	}

	public static FloatBuffer createFloatBuffer(int capacity) {
		return createByteBuffer(capacity * 4).asFloatBuffer();
	}

	public static ByteBuffer createByteBuffer(int capacity) {
		return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
	}

	public static ShortBuffer createShortBuffer(int capacity) {
		return createByteBuffer(capacity * 2).asShortBuffer();
	}
}
