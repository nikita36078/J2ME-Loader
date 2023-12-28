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

public class ByteBuffer extends Buffer {
	private ByteBuffer() {
	}

	public static ByteBuffer allocateDirect(int size) {
		return new ByteBuffer();
	}

	public static ByteBuffer allocateDirect(ByteBuffer buffer) {
		return buffer;
	}

	public byte[] get(int srcIndex, byte[] buf, int dstIndex, int length) {
		return buf;
	}

	public void put(int dstIndex, byte[] buf, int srcIndex, int length) {
	}
}
