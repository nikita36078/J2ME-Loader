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

import android.os.Build;

public abstract class Buffer {
	final java.nio.Buffer buffer;
	int offset;
	int length;

	Buffer(java.nio.Buffer buffer) {
		this.buffer = buffer;
		length = buffer.capacity();
	}

	public int length() {
		return length;
	}

	public synchronized void setBounds(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	public synchronized void resetBounds() {
		offset = 0;
		length = buffer.capacity();
	}

	final java.nio.Buffer getNioBuffer() {
		buffer.rewind();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			return buffer.slice(offset, length);
		} else {
			return buffer.position(offset).limit(length);
		}
	}
}
