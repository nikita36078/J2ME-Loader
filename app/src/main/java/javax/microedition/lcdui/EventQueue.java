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

package javax.microedition.lcdui;

import javax.microedition.util.LinkedEntry;
import javax.microedition.util.LinkedList;

/**
 * Очередь событий. В целом очень хитрая штука...
 */
public class EventQueue implements Runnable
{
	protected LinkedList<Event> queue;
	protected Event event;
	
	protected boolean enabled;
	protected Thread thread;
	
	protected final Object waiter;
	protected final Object interlock;
	
	protected boolean isrunning;
	protected boolean continuerun;
	
	protected boolean immediate;
	
	public EventQueue()
	{
		queue = new LinkedList();
		
		waiter = new Object();
		interlock = new Object();
		
		immediate = false;
	}
	
	/**
	 * Включить неотложный режим обработки.
	 * 
	 * В этом режиме события обрабатываются сразу при поступлении,
	 * очереди как таковой нет (нарушается принцип сериализации).
	 * 
	 * Можно попробовать включить этот режим, если каждый FPS на счету,
	 * но как себя при этом поведет мидлет - нужно смотреть индивидуально.
	 * 
	 * @param value должен ли быть включен неотложный режим
	 */
	public void setImmediate(boolean value)
	{
		immediate = value;
	}
	
	/**
	 * Проверить, включен ли неотложный режим обработки.
	 * @return
	 */
	public boolean isImmediate()
	{
		return immediate;
	}
	
	/**
	 * Добавить событие в очередь.
	 * 
	 * Если включен неотложный режим обработки,
	 * событие обрабатывается здесь же,
	 * в этом случае очереди как таковой и нет.
	 * 
	 * Если событие было добавлено в очередь,
	 * вызывается его метод enterQueue().
	 * 
	 * @param event добавляемое событие
	 */
	public void postEvent(Event event)
	{
//		System.out.println("Post event " + event.getID());
		
		if(immediate)		// включен неотложный режим
		{
			event.run();	// обрабатываем событие на месте
			return;			// и больше нам здесь ловить нечего
		}
		
		boolean empty;
		
		synchronized(queue)	// все операции с очередью должны быть синхронизированы (на ней самой)
		{
			empty = queue.isEmpty();
			
			if(empty || event.placeableAfter(queue.getLast()))
			{
				/*
				 * Если собственно очередь пустая, то это уже подразумевает, что осталось
				 * либо ровно одно событие и оно сейчас в обработке,
				 * либо не осталось вообще ни одного события.
				 * 
				 * И в том, и в другом случае новое событие следует добавить в очередь,
				 * независимо от значения event.placeableAfter().
				 */
				
				queue.addLast(event);
				event.enterQueue();
			}
			else
			{
				// так правильнее, но требуются дополнительные проверки
				// queue.setLast(event).recycle(); // предыдущее убрать, а это - добавить
				
				event.recycle(); // так надежнее // оставить предыдущее, новое сдать в утиль
			}
			
//			queue.dump(System.out);
		}
		
		if(empty)
		{
			/**
			 * с другой стороны, если очередь была непустая,
			 * то как минимум еще на одну итерацию события есть,
			 * и этого делать не нужно
			 */
			
			synchronized(waiter)
			{
				if(isrunning)
				{
					continuerun = true;
				}
				else
				{
					waiter.notifyAll();
				}
			}
		}
	}
	
	/**
	 * Удалить из очереди события, подходящие под заданный фильтр.
	 * 
	 * @param filter фильтр событий для удаления
	 * @return true, если что-то было удалено
	 */
	public boolean removeEvents(EventFilter filter)
	{
		if(queue.isEmpty())
		{
			return false;
		}
		
		boolean removed = false;
		
		synchronized(queue)
		{
//			System.out.println("removeEvents start");
			
			LinkedEntry<Event> entry = queue.firstEntry();
			LinkedEntry<Event> last = queue.lastEntry();
			LinkedEntry<Event> next;
			
			while(true)
			{
//				queue.dump(System.out);
				
				next = entry.nextEntry();
				
				if(filter.accept(entry.getElement()))
				{
					queue.recycleEntry(entry);
					removed = true;
				}
				
				if(entry != last)
				{
					entry = next;
				}
				else
				{
					break;
				}
			}
			
//			System.out.println("removeEvents end");
		}
		
		return removed;
	}
	
	/**
	 * Проверить, есть ли что-нибудь в очереди.
	 * @return true, если очередь пуста
	 */
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}
	
	/**
	 * Очистить очередь.
	 */
	public void clear()
	{
		synchronized(queue)
		{
			queue.clear();
		}
	}
	
	/**
	 * Запустить цикл обработки событий.
	 * Повторные вызовы этого метода игнорируются.
	 */
	public void startProcessing()
	{
		enabled = true;
		
		if(thread == null)
		{
			thread = new Thread(this);
			thread.start();
		}
	}
	
	/**
	 * Остановить цикл обработки событий.
	 * Этот метод блокируется до полной остановки цикла.
	 */
	public void stopProcessing()
	{
		enabled = false;
		
		synchronized(waiter)
		{
			waiter.notifyAll();
		}
		
		synchronized(interlock)
		{
			thread = null;
		}
	}
	
	/**
	 * @return текущее обрабатываемое событие, или null
	 */
	public Event currentEvent()
	{
		return event;
	}
	
	/**
	 * Здесь крутится основной цикл обработки событий.
	 */
	public void run()
	{
		synchronized(interlock) 
		{
			isrunning = true;
			
			while(enabled)
			{
				/*
				 * порядок блокировки:
				 * 
				 * 1 - this
				 * 2 - queue
				 * 
				 * соответственно, в Canvas.serviceRepaints() порядок должен быть такой же,
				 * иначе возможна взаимная блокировка двух потоков (все повиснет)
				 */
				
				synchronized(this)		// нужно для Canvas.serviceRepaints()
				{
					synchronized(queue)	// нужно для postEvent()
					{
						event = queue.removeFirst();	// достаем первый элемент и сразу же удаляем из очереди
					}
					
					// event = queue.getFirst();
				}
				
				if(event != null)
				{
					try
					{
						event.process();
					}
					catch(Throwable ex)
					{
						ex.printStackTrace();
					}
					
					synchronized(queue)
					{
						// queue.removeFirst();
						
						event.leaveQueue();
						event.recycle();
					}
					
//					System.out.println("Event " + event.getID() + " processed, removed from queue and recycled");
					
					synchronized(this)
					{
						synchronized(queue)
						{
							event = null;
						}
						
						this.notifyAll();
					}
				}
				else
				{
					synchronized(waiter)
					{
						if(continuerun)
						{
							continuerun = false;
						}
						else
						{
							isrunning = false;
							
							try
							{
								waiter.wait();
							}
							catch(InterruptedException ie)
							{
							}
							
							isrunning = true;
						}
					}
				}
			}
		}
	}

//	public void uncaughtException(Thread thread, Throwable ex)
//	{
//		System.err.println("Uncaught exception while processing event " + (event != null ? event.getID() : "null"));
//		ex.printStackTrace();
//		
//		System.exit(1);
//	}
}