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

package javax.microedition.lcdui.event;

import javax.microedition.lcdui.Event;
import javax.microedition.util.ArrayStack;

public class RunnableEvent extends Event
{
	private static ArrayStack<RunnableEvent> recycled = new ArrayStack();
	
	private Runnable runnable;
	
	private RunnableEvent()
	{
	}
	
	public static Event getInstance(Runnable runnable)
	{
		RunnableEvent instance = recycled.pop();
		
		if(instance == null)
		{
			instance = new RunnableEvent();
		}
		
		instance.runnable = runnable;
		
		return instance;
	}
	
	public void process()
	{
		runnable.run();
	}
	
	public void recycle()
	{
		runnable = null;
		recycled.push(this);
	}
	
	public void enterQueue()
	{
	}
	
	public void leaveQueue()
	{
	}
	
	public boolean placeableAfter(Event event)
	{
		return true;
	}
}