/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017 Nikita Shakarun
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

package javax.microedition.util;

public class ArrayStack<E> {
	public static final int DELTA = 100;

	private Object[] data;
	private int index;

	public ArrayStack() {
		clear();
	}

	public synchronized void push(E value) {
		if (index >= data.length - 1) {
			Object[] temp = new Object[data.length + DELTA];
			System.arraycopy(data, 0, temp, 0, data.length);
			data = temp;
		}

		data[++index] = value;
	}

	public synchronized E pop() {
		if (index < 0) {
			return null;
		}

		//noinspection unchecked
		return (E) data[index--];
	}

	public synchronized void clear() {
		data = new Object[0];
		index = -1;
	}
}
