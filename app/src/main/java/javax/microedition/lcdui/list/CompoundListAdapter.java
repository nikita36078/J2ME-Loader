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

package javax.microedition.lcdui.list;

import javax.microedition.lcdui.Choice;

import android.R.layout;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListAdapter;

public class CompoundListAdapter extends CompoundAdapter implements ListAdapter
{
	protected int listType;
	protected int viewResourceID;
	protected ItemSelector selector;
	
	public CompoundListAdapter(Context context, ItemSelector selector, int type)
	{
		super(context);
		
		this.listType = type;
		this.selector = selector;
		
		switch(type)
		{
			case Choice.IMPLICIT:
				viewResourceID = layout.simple_list_item_1;
				break;
				
			case Choice.EXCLUSIVE:
				viewResourceID = layout.simple_list_item_single_choice;
				break;
				
			case Choice.MULTIPLE:
				viewResourceID = layout.simple_list_item_multiple_choice;
				break;
				
			default:
				throw new IllegalArgumentException("list type " + type + " is not supported");
		}
		
		if(type != Choice.IMPLICIT && selector == null)
		{
			throw new IllegalArgumentException("ItemSelector is requered for this list type");
		}
	}
	
	public View getView(int position, View convertView, ViewGroup parent)
	{
		convertView = getView(position, convertView, parent, viewResourceID, true);
		
		if(listType != Choice.IMPLICIT && convertView instanceof CompoundButton)
		{
			((CompoundButton)convertView).setChecked(selector.isSelected(position));
		}
		
		return convertView;
	}

	public boolean areAllItemsEnabled()
	{
		return true;
	}

	public boolean isEnabled(int position)
	{
		return true;
	}
}