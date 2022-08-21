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

/**
 * The doubly linked list.
 *
 * @param <E> what it contains
 */
public class LinkedList<E> {
	private final ArrayStack<LinkedEntry<E>> pool = new ArrayStack<>();
	private final LinkedEntry<E> head = new LinkedEntry<>();
	private final LinkedEntry<E> tail = new LinkedEntry<>();

	public LinkedList() {
		head.insertBefore(tail);
	}

	/**
	 * Check if there is anything in the list.
	 *
	 * @return true, if the list is empty
	 */
	public boolean isEmpty() {
		return head.nextEntry() == tail;
	}

	/**
	 * Clear the list
	 */
	public void clear() {
		/*
		 * disconnect the head and tail from the previous list
		 */

		head.remove();
		tail.remove();

		head.insertBefore(tail);    // form a new list from the head and tail
	}

	/**
	 * @return the first list item
	 */
	public LinkedEntry<E> firstEntry() {
		return head.nextEntry();
	}

	/**
	 * @return the last list item
	 */
	public LinkedEntry<E> lastEntry() {
		return tail.prevEntry();
	}

	/**
	 * @return the first value in the list
	 */
	public E getFirst() {
		return head.nextEntry().getElement();
	}

	/**
	 * Delete the first value in the list.
	 *
	 * @return the previous first value in the list; null if no such value
	 */
	public E removeFirst() {
		LinkedEntry<E> entry = head.nextEntry();

		if (entry != tail) {
			return recycleEntry(entry);
		} else {
			return null;
		}
	}

	/**
	 * @return the last value in the list
	 */
	public E getLast() {
		return tail.prevEntry().getElement();
	}

	/**
	 * Add value to the end of the list.
	 *
	 * @param element the new value
	 */
	public void addLast(E element) {
		getEntryInstance(element).insertBefore(tail);
	}

	/**
	 * Get an instance of LinkedEntry.
	 * <p>
	 * Here is the optimization:
	 * if we have free copies in the pool, we get them from the pool;
	 * if not - create a new instance.
	 *
	 * @param element the value to initialize the instance
	 * @return the instance of LinkedEntry
	 */
	public LinkedEntry<E> getEntryInstance(E element) {
		LinkedEntry<E> entry = pool.pop();

		if (entry == null) {
			entry = new LinkedEntry<>();
		}

		entry.setElement(element);

		return entry;
	}

	/**
	 * Recycle an LinkedEntry instance
	 * <p>
	 * In this case, the copy is reset and returned to the pool.
	 *
	 * @param entry the LinkedEntry instance for recycling
	 * @return the value contained in this instance
	 */
	public E recycleEntry(LinkedEntry<E> entry) {
		E element = entry.getElement();

		entry.remove();
		entry.setElement(null);

		pool.push(entry);

		return element;
	}
}