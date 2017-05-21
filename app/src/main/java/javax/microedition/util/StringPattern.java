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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Класс для сравнения строк на соответствие шаблону.
 *
 * В шаблонах возможно использование спецсимволов:
 * '*' - заменяет ноль или более любых символов,
 * ';' - служит для разделения нескольких шаблонов.
 *
 * @author SilentKnight
 * @version 2.0
 */
public class StringPattern
{
	public static final char WILDCARD = '*';
	public static final char SEPARATOR = ';';
	
	protected String[] prefix;
	protected String[] suffix;
	protected ArrayList<String>[] middle;

	protected boolean ignorecase;
	protected int count;

	protected int hashcode;
	
	/**
	 * Создать строковый шаблон.
	 *
	 * @param pattern собственно спецификатор шаблона
	 * @param ignorecase true, если не нужно учитывать регистр при сравнении
	 */
	public StringPattern(String pattern, boolean ignorecase)
	{
		this.ignorecase = ignorecase;
		
		if(ignorecase)
		{
			pattern = pattern.toLowerCase();
		}

		hashcode = pattern.hashCode();
		
		ArrayList<String> patterns = tokenizeString(pattern, SEPARATOR);
		count = patterns.size();
		
		prefix = new String[count];
		suffix = new String[count];
		middle = new ArrayList[count];
		
		for(int i = 0; i < count; i++)
		{
			pattern = patterns.get(i);
			
			int index = pattern.indexOf(WILDCARD);
			
			if(index < 0)
			{
				prefix[i] = pattern;
				pattern = "";
			}
			else
			{
				prefix[i] = pattern.substring(0, index);
				pattern = pattern.substring(index + 1);
			}
			
			index = pattern.lastIndexOf(WILDCARD);
			
			if(index < 0)
			{
				suffix[i] = pattern;
				pattern = "";
			}
			else
			{
				suffix[i] = pattern.substring(index + 1, pattern.length());
				pattern = pattern.substring(0, index);
			}
			
			middle[i] = tokenizeString(pattern, WILDCARD);
		}
	}
	
	/**
	 * Проверить строку на соотвествие шаблону.
	 *
	 * Если шаблон состоит из нескольких частей (разделенных ';'), то строка
	 * проходит проверку, если она соответствует хотя бы одной части.
	 *
	 * @param str проверяемая строка
	 * @return true, ести строка соответствует шаблону или его части
	 */
	public boolean matchesWith(String str)
	{
		if(ignorecase)
		{
			str = str.toLowerCase();
		}
		
		for(int i = 0; i < count; i++)
		{
			boolean flag = str.startsWith(prefix[i]);
			
			if(flag)
			{
				str = str.substring(prefix[i].length());
				
				if(str.length() > 0)
				{
					flag = str.endsWith(suffix[i]);
					
					if(flag)
					{
						str = str.substring(0, str.length() - suffix[i].length());
						
						Iterator<String> mid = middle[i].iterator();
						String cmp;
						int index;
						
						while(str.length() > 0 && mid.hasNext())
						{
							cmp = mid.next();
							index = str.indexOf(cmp);
							
							if(index >= 0)
							{
								str = str.substring(index + cmp.length());
							}
							else
							{
								flag = false;
								break;
							}
						}
					}
				}
			}
			
			if(flag)
			{
				return true;
			}
		}
		
		return false;
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

		final StringPattern other = (StringPattern)obj;

		if(this.hashcode != other.hashcode)
		{
			return false;
		}
		
		return true;
	}

	public int hashCode()
	{
		return hashcode;
	}
	
	/**
	 * Разбить строку на подстроки.
	 *
	 * Несколько последовательных разделителей считаются за один.
	 * В результат разбиения разделители не включаюся.
	 *
	 * @param str разделяемая строка
	 * @param sep символ-разделитель
	 * @return вектор с результатами разбиения
	 */
	public static ArrayList<String> tokenizeString(String str, char sep)
	{
		ArrayList<String> res = new ArrayList();
		int index;
		
		while(str.length() > 0)
		{
			index = str.indexOf(sep);
			
			if(index > 0)
			{
				res.add(str.substring(0, index));
				str = str.substring(index + 1);
			}
			else if(index == 0)
			{
				str = str.substring(1);
			}
			else
			{
				res.add(str);
				break;
			}
		}
		
		return res;
	}
}
