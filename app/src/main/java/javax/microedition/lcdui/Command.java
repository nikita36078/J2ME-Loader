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

public class Command implements Comparable<Command>
{
	public static final int SCREEN = 1;
	public static final int BACK = 2;
	public static final int CANCEL = 3;
	public static final int OK = 4;
	public static final int HELP = 5;
	public static final int STOP = 6;
	public static final int EXIT = 7;
	public static final int ITEM = 8;
	
	private String shortLabel;
	private String longLabel;
	private int commandType;
	private int priority;
	
	public Command(String label, int commandType, int priority)
	{
		this(label, null, commandType, priority);
	}
	
	public Command(String shortLabel, String longLabel, int commandType, int priority)
	{
		this.shortLabel = shortLabel;
		this.longLabel = longLabel;
		this.commandType = commandType;
		this.priority = priority;
	}
	
	public String getLabel()
	{
		return shortLabel;
	}
	
	public String getLongLabel()
	{
		return longLabel;
	}
	
	public int getCommandType()
	{
		return commandType;
	}
	
	public int getPriority()
	{
		return priority;
	}
	
	public int hashCode()
	{
		int hash = 3;
		
		hash = 97 * hash + (shortLabel != null ? shortLabel.hashCode() : 0);
		hash = 97 * hash + (longLabel != null ? longLabel.hashCode() : 0);
		hash = 97 * hash + commandType;
		hash = 97 * hash + priority;
		
		return hash;
	}

	public boolean equals(Object obj)
	{
		if(obj == null)
		{
			return false;
		}
		
		if(getClass() != obj.getClass())
		{
			return false;
		}
		
		final Command other = (Command) obj;
		
		if((shortLabel == null) ? (other.shortLabel != null) : !shortLabel.equals(other.shortLabel))
		{
			return false;
		}
		
		if((longLabel == null) ? (other.longLabel != null) : !longLabel.equals(other.longLabel))
		{
			return false;
		}
		
		if(commandType != other.commandType)
		{
			return false;
		}
		
		if(priority != other.priority)
		{
			return false;
		}
		
		return true;
	}

	public int compareTo(Command cmd)
	{
		return cmd.getPriority() - priority;
	}
	
	public String toString()
	{
		return "Command(\"" + shortLabel + "\", " + commandType + ", " + priority + ")";
	}
}