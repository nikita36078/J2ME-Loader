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
 * Двусвязный список.
 *
 * @param <E> из чего он составлен
 */
public class LinkedList<E> {
	private ArrayStack<LinkedEntry<E>> pool;

	private final LinkedEntry<E> head;
	private final LinkedEntry<E> tail;

	public LinkedList() {
		pool = new ArrayStack<>();

		head = new LinkedEntry<>();
		tail = new LinkedEntry<>();

		head.insertBefore(tail);
	}

	/**
	 * Проверить, есть ли что в списке.
	 *
	 * @return true, если список пуст
	 */
	public boolean isEmpty() {
		return head.nextEntry() == tail;
	}

	/**
	 * Очистить список
	 */
	public void clear() {
		/*
		 * отсоединяем голову и хвост от предыдущего списка
		 */

		head.remove();
		tail.remove();

		head.insertBefore(tail);    // образуем из головы и хвоста новый список
	}

	/**
	 * @return первый элемент списка
	 */
	public LinkedEntry<E> firstEntry() {
		return head.nextEntry();
	}

	/**
	 * @return последний элемент списка
	 */
	public LinkedEntry<E> lastEntry() {
		return tail.prevEntry();
	}

	/**
	 * @return первое значение в списке
	 */
	public E getFirst() {
		return head.nextEntry().getElement();
	}

	/**
	 * Удалить первое значение в списке.
	 *
	 * @return бывшее первое значение в списке; null, если такого значения не было
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
	 * @return последнее значение в списке
	 */
	public E getLast() {
		return tail.prevEntry().getElement();
	}

	/**
	 * Добавить значение в конец списка.
	 *
	 * @param element новое значение
	 */
	public void addLast(E element) {
		getEntryInstance(element).insertBefore(tail);
	}

	/**
	 * Достать экземпляр LinkedEntry.
	 * <p>
	 * Здесь оптимизация:
	 * если у нас есть свободные экземпляры в пуле, достаем из пула;
	 * если свободных нет, так и быть, создаем новый экземпляр.
	 *
	 * @param element значение, которым инициализировать экземпляр
	 * @return экземпляр LinkedEntry
	 */
	public LinkedEntry<E> getEntryInstance(E element) {
		LinkedEntry<E> entry = pool.pop();

		if (entry == null) {
			entry = new LinkedEntry();
		}

		entry.setElement(element);

		return entry;
	}

	/**
	 * Сдать экземпляр LinkedEntry в утиль.
	 * <p>
	 * При этом экземпляр обнуляется и возвращается в пул.
	 *
	 * @param entry экземпляр LinkedEntry для утилизации
	 * @return значение, содержавшееся в этом экземпляре
	 */
	public E recycleEntry(LinkedEntry<E> entry) {
		E element = entry.getElement();

		entry.remove();
		entry.setElement(null);

		pool.push(entry);

		return element;
	}
}