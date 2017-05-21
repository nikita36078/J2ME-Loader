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

/**
 * Базовый класс для всех событий.
 */
public abstract class Event implements Runnable
{
//	private static int count = 0;
//	private int id = count++;
//	
//	public int getID()
//	{
//		return id;
//	}
	
	/**
	 * Обработка события.
	 * Именно здесь нужно выполнять требуемые действия.
	 */
	public abstract void process();
	
	/**
	 * Сдача события в утиль.
	 * 
	 * Если предусмотрен пул событий, то здесь
	 * событие нужно обнулить и вернуть в пул.
	 */
	public abstract void recycle();
	
	/**
	 * Обработать событие и сдать в утиль за один прием.
	 */
	public void run()
	{
		process();
		recycle();
	}
	
	/**
	 * Вызывается, когда событие вошло в очередь.
	 * Здесь можно увеличить счетчик таких событий в очереди.
	 */
	public abstract void enterQueue();
	
	/**
	 * Вызывается, когда событие покинуло очередь.
	 * Здесь можно уменьшить счетчик таких событий в очереди.
	 */
	public abstract void leaveQueue();
	
	/**
	 * Проверить, можно ли поместить это событие в очередь
	 * сразу за некоторым другим событием.
	 * 
	 * @param event событие, после которого нас могут поместить в очередь
	 * @return true, если мы на это согласны
	 */
	public abstract boolean placeableAfter(Event event);
}