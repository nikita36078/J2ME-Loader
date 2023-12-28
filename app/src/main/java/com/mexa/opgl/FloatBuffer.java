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

public class FloatBuffer extends Buffer {
	private FloatBuffer() {
	}

	public static FloatBuffer allocateDirect(int size) {
		return new FloatBuffer();
	}

	public static FloatBuffer allocateDirect(FloatBuffer buffer) {
		return buffer;
	}

	public float[] get(int srcIndex, float[] buf, int dstIndex, int length) {
		return buf;
	}

	public void put(int dstIndex, float[] buf, int srcIndex, int length) {
	}
}
