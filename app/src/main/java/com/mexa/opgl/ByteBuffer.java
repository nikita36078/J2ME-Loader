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

package com.mexa.opgl;

import ru.woesss.j2me.micro3d.BufferUtils;

public class ByteBuffer extends Buffer {

	private ByteBuffer(int size) {
		super(BufferUtils.createByteBuffer(size));
	}

	private ByteBuffer(ByteBuffer buffer) {
		this(buffer.length());
		java.nio.ByteBuffer nio = (java.nio.ByteBuffer) super.buffer;
		nio.put((java.nio.ByteBuffer) buffer.getNioBuffer());
	}

	public static ByteBuffer allocateDirect(int size) {
		return new ByteBuffer(size);
	}

	public static ByteBuffer allocateDirect(ByteBuffer buffer) {
		return new ByteBuffer(buffer);
	}

	public byte[] get(int srcIndex, byte[] buf, int dstIndex, int length) {
		java.nio.ByteBuffer nio = (java.nio.ByteBuffer) super.buffer;
		nio.position(srcIndex);
		nio.get(buf, dstIndex, length);
		return buf;
	}

	public void put(int dstIndex, byte[] buf, int srcIndex, int length) {
		java.nio.ByteBuffer nio = (java.nio.ByteBuffer) super.buffer;
		nio.position(dstIndex);
		nio.put(buf, srcIndex, length);
	}
}
