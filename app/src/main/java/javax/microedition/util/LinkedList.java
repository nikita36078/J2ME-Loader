/*
 * Copyright 2012 Kulikov Dmitriy
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
 * @param <E> из чего он составлен
 */
public class LinkedList<E>
{
	protected ArrayStack<LinkedEntry<E>> pool;
	
	protected final LinkedEntry<E> head;
	protected final LinkedEntry<E> tail;
	
	public LinkedList()
	{
		pool = new ArrayStack();
		
		head = new LinkedEntry();
		tail = new LinkedEntry();
		
		head.insertBefore(tail);
	}
	
	/**
	 * Проверить, есть ли что в списке.
	 * @return true, если список пуст
	 */
	public boolean isEmpty()
	{
		return head.nextEntry() == tail;
	}
	
	/**
	 * Очистить список
	 */
	public void clear()
	{
		/*
		 * отсоединяем голову и хвост от предыдущего списка
		 */
		
		head.remove();
		tail.remove();
		
		head.insertBefore(tail);	// образуем из головы и хвоста новый список
	}
	
	/**
	 * @return первый элемент списка
	 */
	public LinkedEntry<E> firstEntry()
	{
		return head.nextEntry();
	}
	
	/**
	 * @return последний элемент списка
	 */
	public LinkedEntry<E> lastEntry()
	{
		return tail.prevEntry();
	}
	
	/**
	 * @return первое значение в списке
	 */
	public E getFirst()
	{
		return head.nextEntry().getElement();
	}
	
	/**
	 * Задать первое значение в списке.
	 * 
	 * @param element новое значение для первого элемента
	 * @return предыдущее значение; null, если его не было
	 */
	public E setFirst(E element)
	{
		LinkedEntry<E> entry = head.nextEntry();
		
		if(entry != tail) // за головой не сразу хвост идет (список не пустой)
		{
			/*
			 * там что-то было, значит,
			 * заменяем это что-то на новое значение
			 */
			
			E former = entry.getElement();
			entry.setElement(element);
			
			return former;
		}
		else // за головой идет сразу хвост (список пустой)
		{
			/*
			 * голову с хвостом не трогаем,
			 * вставляем новый элемент
			 */
			
			addFirst(element);
			return null;
		}
	}
	
	/**
	 * Добавить значение в начало списка.
	 * @param element новое значение
	 */
	public void addFirst(E element)
	{
		getEntryInstance(element).insertAfter(head);
	}
	
	/**
	 * Удалить первое значение в списке.
	 * @return бывшее первое значение в списке; null, если такого значения не было
	 */
	public E removeFirst()
	{
		LinkedEntry<E> entry = head.nextEntry();
		
		if(entry != tail)
		{
			return recycleEntry(entry);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * @return последнее значение в списке
	 */
	public E getLast()
	{
		return tail.prevEntry().getElement();
	}
	
	/**
	 * Задать последнее значение в списке.
	 * 
	 * @param element новое значение для последнего элемента
	 * @return предыдущее значение; null, если его не было
	 */
	public E setLast(E element)
	{
		LinkedEntry<E> entry = tail.prevEntry();
		
		if(entry != head) // перед хвостом не сразу голова идет (список не пустой)
		{
			/*
			 * там что-то было, значит,
			 * заменяем это что-то на новое значение
			 */
			
			E former = entry.getElement();
			entry.setElement(element);
			
			return former;
		}
		else // перед хвостом идет сразу голова (список пустой)
		{
			/*
			 * голову с хвостом не трогаем,
			 * вставляем новый элемент
			 */
			
			addFirst(element);
			return null;
		}
	}
	
	/**
	 * Добавить значение в конец списка.
	 * @param element новое значение
	 */
	public void addLast(E element)
	{
		getEntryInstance(element).insertBefore(tail);
	}
	
	/**
	 * Удалить последнее значение в списке.
	 * @return бывшее последнее значение в списке; null, если такого значения не было
	 */
	public E removeLast()
	{
		LinkedEntry<E> entry = tail.prevEntry();
		
		if(entry != head)
		{
			return recycleEntry(entry);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Достать экземпляр LinkedEntry.
	 * 
	 * Здесь оптимизация:
	 * если у нас есть свободные экземпляры в пуле, достаем из пула;
	 * если свободных нет, так и быть, создаем новый экземпляр.
	 * 
	 * @param element значение, которым инициализировать экземпляр
	 * @return экземпляр LinkedEntry
	 */
	public LinkedEntry<E> getEntryInstance(E element)
	{
		LinkedEntry<E> entry = pool.pop();
		
		if(entry == null)
		{
			entry = new LinkedEntry();
		}
		
		entry.setElement(element);
		
		return entry;
	}
	
	/**
	 * Сдать экземпляр LinkedEntry в утиль.
	 * 
	 * При этом экземпляр обнуляется и возвращается в пул.
	 * 
	 * @param entry экземпляр LinkedEntry для утилизации
	 * @return значение, содержавшееся в этом экземпляре
	 */
	public E recycleEntry(LinkedEntry<E> entry)
	{
		E element = entry.getElement();
		
		entry.remove();
		entry.setElement(null);
		
		pool.push(entry);
		
		return element;
	}
	
//	public void dump(PrintStream ps)
//	{
//		LinkedEntry<E> entry = head;
//		ps.println("Beginning list dump...");
//		
//		while(true)
//		{
//			ps.println(entry);
//			entry = entry.nextEntry();
//			
//			if(entry == tail)
//			{
//				ps.println(entry);
//				break;
//			}
//		}
//	}
}