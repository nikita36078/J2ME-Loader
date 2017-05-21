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

public interface DataContainer
{
	public DataEditor edit();
	
	public boolean contains(String key);
	
	public boolean getBoolean(String key, boolean defValue);
	public float getFloat(String key, float defValue);
	public int getInt(String key, int defValue);
	public long getLong(String key, long defValue);
	public String getString(String key, String defValue);
	
	public boolean getBoolean(String key);
	public float getFloat(String key);
	public int getInt(String key);
	public long getLong(String key);
	public String getString(String key);
}