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

package javax.microedition.lcdui.pointer;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.event.CanvasEvent;

public class KeyRepeater implements Runnable
{
	public static long[] INTERVALS =
	{
		400,
		200,
		400,
		128,
		128,
		128,
		128,
		128,
		80
	};
	
	protected Canvas target;
	
	protected Thread thread;
	protected Object waiter;
	protected boolean isrunning;
	
	protected boolean enabled;
	protected int position;
	
	protected int keyCode;
	protected int secondKeyCode;
	
	public KeyRepeater()
	{
		waiter = new Object();
		
		thread = new Thread(this);
		thread.start();
	}
	
	public void setTarget(Canvas canvas)
	{
		if(canvas == null)
		{
			stop();
		}
		
		target = canvas;
	}
	
	public void start(int keyCode)
	{
		start(keyCode, 0);
	}
	
	public void start(int keyCode, int secondKeyCode)
	{
		if(target == null)
		{
			return;
		}
		
		synchronized(waiter)
		{
			if(isrunning)
			{
				return;
			}
			
			this.keyCode = keyCode;
			this.secondKeyCode = secondKeyCode;
			
			enabled = true;
			position = 0;
			
			waiter.notifyAll();
		}
	}
	
	public void stop()
	{
		enabled = false;
		thread.interrupt();
	}
	
	public boolean isRunning()
	{
		return isrunning;
	}
	
	public void run()
	{
		while(true)
		{
			try
			{
				synchronized(waiter)
				{
					isrunning = false;
					waiter.wait();
					
					isrunning = true;
				}
				
				while(enabled)
				{
					Thread.sleep(INTERVALS[position]);
					
					target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_REPEATED, keyCode));
					
					if(secondKeyCode != 0)
					{
						target.postEvent(CanvasEvent.getInstance(target, CanvasEvent.KEY_REPEATED, secondKeyCode));
					}
					
					if(position < INTERVALS.length - 1)
					{
						position++;
					}
				}
			}
			catch(InterruptedException ie)
			{
			}
		}
	}
}