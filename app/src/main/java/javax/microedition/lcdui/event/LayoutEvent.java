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

import android.view.View;
import android.view.ViewGroup;

public class LayoutEvent extends Event
{
	private static ArrayStack<LayoutEvent> recycled = new ArrayStack();
	
	private static final int ADD_VIEW = 0;
	
	private int eventType;
	private ViewGroup layout;
	private View view;
	
	public static Event addView(ViewGroup layout, View view)
	{
		LayoutEvent instance = recycled.pop();
		
		if(instance == null)
		{
			instance = new LayoutEvent();
		}
		
		instance.eventType = ADD_VIEW;
		
		instance.layout = layout;
		instance.view = view;
		
		return instance;
	}
	
	public void process()
	{
		switch(eventType)
		{
			case ADD_VIEW:
				layout.addView(view);
				return;
		}
	}
	
	public void recycle()
	{
		layout = null;
		view = null;
		
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