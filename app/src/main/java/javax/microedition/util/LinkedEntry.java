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
 * Элемент связного списка.
 * @param <E> что он содержит
 */
public class LinkedEntry<E>
{
	private LinkedEntry<E> prev;
	private LinkedEntry<E> next;
	
	private E element;
	
//	private static int counter = 0;
//	private int id;
//	
//	public LinkedEntry()
//	{
//		id = ++counter;
//	}
	
	/**
	 * Присвоить элементу значение.
	 * @param element новое значение
	 */
	public void setElement(E element)
	{
		this.element = element;
	}
	
	/**
	 * @return текущее значение элемента
	 */
	public E getElement()
	{
		return element;
	}
	
	/**
	 * @return элемент, предшествующий данному; или null, если такого элемента нет
	 */
	public LinkedEntry<E> prevEntry()
	{
		return prev;
	}
	
	/**
	 * @return элемент, следующий за данным; null, если такого элемента нет
	 */
	public LinkedEntry<E> nextEntry()
	{
		return next;
	}
	
	/**
	 * Изъять этот элемент из списка.
	 * Смежные элементы при этом соединяются между собой.
	 */
	public void remove()
	{
		if(prev != null)
		{
			prev.next = next;	// следующий за предыдущим = следующий за этим
		}
		
		if(next != null)
		{
			next.prev = prev;	// предшествующий следующему = предшествующий этому
		}
		
		prev = null;
		next = null;
	}
	
	/**
	 * Восстановить двунаправленность связей для этого элемента.
	 * 
	 * То есть, если перед нами кто-то есть, то мы стоим за ним;
	 * если кто-то есть после нас, то мы стоим перед ним.
	 */
	private void updateLinks()
	{
		if(prev != null)
		{
			prev.next = this;
		}
		
		if(next != null)
		{
			next.prev = this;
		}
	}
	
	/**
	 * Вставить этот элемент перед указанным.
	 * Предполагается, что указанный элемент входит в состав некоторого списка.
	 * 
	 * @param entry 
	 */
	public void insertBefore(LinkedEntry<E> entry)
	{
		remove();			// элемент не можеть быть одновременно в двух местах в списке
		
		prev = entry.prev;	// предыдущий для указанного - теперь наш предыдущий
		next = entry;		// сам указанный - теперь наш следующий
		
		updateLinks();		// доносим эти изменения до наших новых соседей
	}
	
	/**
	 * Вставить этот элемент после указанного.
	 * Предполагается, что указанный элемент входит в состав некоторого списка.
	 * 
	 * @param entry
	 */
	public void insertAfter(LinkedEntry<E> entry)
	{
		remove();			// элемент не можеть быть одновременно в двух местах в списке
		
		prev = entry;		// сам указанный - теперь наш предыдущий
		next = entry.next;	// следующий для указанного - теперь наш следующий
		
		updateLinks();		// доносим эти изменения до наших новых соседей
	}
	
//	public String toString()
//	{
//		StringBuilder buf = new StringBuilder();
//		
//		buf.append("LinkedEntry(");
//		
//		if(prev != null)
//		{
//			buf.append(prev.id);
//		}
//		else
//		{
//			buf.append(-1);
//		}
//		
//		buf.append(", ");
//		
//		buf.append(id);
//		buf.append(", ");
//		
//		if(next != null)
//		{
//			buf.append(next.id);
//		}
//		else
//		{
//			buf.append(-1);
//		}
//		
//		buf.append(", ");
//		buf.append(element);
//		
//		buf.append(")");
//		
//		return buf.toString();
//	}
}