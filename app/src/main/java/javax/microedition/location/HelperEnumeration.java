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
