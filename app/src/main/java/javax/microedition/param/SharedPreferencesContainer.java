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

package javax.microedition.param;

import javax.microedition.util.ContextHolder;

import android.content.SharedPreferences;
import android.content.Context;

public class SharedPreferencesContainer implements DataContainer, DataEditor
{
	protected SharedPreferences prefs;
	protected SharedPreferences.Editor editor;
	
	public SharedPreferencesContainer(String name, int mode, Context context)
	{
		prefs = context.getSharedPreferences(name, mode);
	}
	
	public SharedPreferencesContainer(SharedPreferences prefs)
	{
		this.prefs = prefs;
	}
	
	public DataEditor edit()
	{
		if(editor == null)
		{
			editor = prefs.edit();
		}
		
		return this;
	}

	public boolean contains(String key)
	{
		return prefs.contains(key);
	}

	public boolean getBoolean(String key, boolean defValue)
	{
		return prefs.getBoolean(key, defValue);
	}

	public float getFloat(String key, float defValue)
	{
		return prefs.getFloat(key, defValue);
	}

	public int getInt(String key, int defValue)
	{
		return prefs.getInt(key, defValue);
	}

	public long getLong(String key, long defValue)
	{
		return prefs.getLong(key, defValue);
	}

	public String getString(String key, String defValue)
	{
		return prefs.getString(key, defValue);
	}

	public boolean getBoolean(String key)
	{
		return prefs.getBoolean(key, false);
	}

	public float getFloat(String key)
	{
		return prefs.getFloat(key, 0);
	}

	public int getInt(String key)
	{
		return prefs.getInt(key, 0);
	}

	public long getLong(String key)
	{
		return prefs.getLong(key, 0);
	}

	public String getString(String key)
	{
		return prefs.getString(key, "");
	}
	
	public DataEditor clear()
	{
		editor.clear();
		return this;
	}

	public DataEditor remove(String key)
	{
		editor.remove(key);
		return this;
	}

	public DataEditor putBoolean(String key, boolean value)
	{
		editor.putBoolean(key, value);
		return this;
	}

	public DataEditor putFloat(String key, float value)
	{
		editor.putFloat(key, value);
		return this;
	}

	public DataEditor putInt(String key, int value)
	{
		editor.putInt(key, value);
		return this;
	}

	public DataEditor putLong(String key, long value)
	{
		editor.putLong(key, value);
		return this;
	}

	public DataEditor putString(String key, String value)
	{
		editor.putString(key, value);
		return this;
	}

	public void apply()
	{
		editor.apply();
	}

	public boolean commit()
	{
		return editor.commit();
	}
	
	public void close()
	{
		editor = null;
	}
}
