/*
 * Copyright 2023 Arman Jussupgaliyev
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
package javax.microedition.location;

import java.util.Enumeration;
import java.util.NoSuchElementException;

class HelperEnumeration implements Enumeration {
	private Object[] elements;
	private int count;

	HelperEnumeration(Object[] elements) {
		this.elements = elements;
		this.count = 0;
	}

	public synchronized boolean hasMoreElements() {
		return (this.elements != null) && (this.count < this.elements.length);
	}

	public synchronized Object nextElement() {
		if (this.count < this.elements.length) {
			return this.elements[(this.count++)];
		}
		throw new NoSuchElementException();
	}
}
