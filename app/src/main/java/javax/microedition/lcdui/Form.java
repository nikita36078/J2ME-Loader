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

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import javax.microedition.util.ContextHolder;

public class Form extends Screen
{
	private ScrollView scrollview;
	private LinearLayout layout;
	
	private ArrayList<Item> items = new ArrayList();
	private ItemStateListener listener;
	
	public Form(String title)
	{
		setTitle(title);
	}
	
	public Form(String title, Item[] elements)
	{
		setTitle(title);
		items.addAll(Arrays.asList(elements));
	}
	
	public Item get(int index)
	{
		return items.get(index);
	}
	
	public int size()
	{
		return items.size();
	}
	
	public int append(String text)
	{
		append(new StringItem(null, text));
		return 0;
	}
	
	public int append(Image img)
	{
		append(new ImageItem(null, img, ImageItem.LAYOUT_DEFAULT, null));
		return 0;
	}
	
	public int append(final Item item)
	{
		items.add(item);
		item.setOwnerForm(this);
		
		if(layout != null)
		{
			// Added by Naik run in UI thread
			MicroActivity a = getParentActivity();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
						public void run() {
							layout.addView(item.getItemView());
						}
					});
			}
		}
		return 0;
	}
	
	public void insert(final int index, final Item item)
	{
		items.add(index, item);
		item.setOwnerForm(this);
		
		if(layout != null)
		{
			// Added by Naik run in UI thread
			MicroActivity a = getParentActivity();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
						public void run() {
							layout.addView(item.getItemView(), index);
						}
				});
			}
		}
	}
	
	public void set(final int index, final Item item)
	{
		items.set(index, item).setOwnerForm(null);
		item.setOwnerForm(this);
		
		if(layout != null)
		{
			// Added by Naik run in UI thread
			MicroActivity a = getParentActivity();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
						public void run() {
							layout.removeViewAt(index);
							layout.addView(item.getItemView(), index);
						}
				});
			}
		}
	}
	
	public void delete(final int index)
	{
		items.remove(index).setOwnerForm(null);
		
		if(layout != null)
		{
			// Added by Naik run in UI thread
			MicroActivity a = getParentActivity();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
						public void run() {
							layout.removeViewAt(index);
						}
					});
			}
		}
	}
	
	public void deleteAll()
	{
		for(Item item : items)
		{
			item.setOwnerForm(null);
		}
		
		items.clear();
		
		if(layout != null)
		{
			// Added by Naik run in UI thread
			MicroActivity a = getParentActivity();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
					public void run() {
			    		layout.removeAllViews();
					}
				});
			}
		}
	}
	
	public void setItemStateListener(ItemStateListener listener)
	{
		this.listener = listener;
	}
	
	public void notifyItemStateChanged(Item item)
	{
		if(listener != null)
		{
			listener.itemStateChanged(item);
		}
	}
	
	public View getScreenView()
	{
		if(scrollview == null)
		{
			Context context = getParentActivity();
			
			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			
			scrollview = new ScrollView(context);
			scrollview.addView(layout);
			
			for(Item item : items)
			{
				layout.addView(item.getItemView());
			}
		}
		
		return scrollview;
	}
	
	public void clearScreenView()
	{
		scrollview = null;
		layout = null;
		
		for(Item item : items)
		{
			item.clearItemView();
		}
	}
	
	public boolean contextMenuItemSelected(MenuItem menuitem)
	{
		for(Item item : items)
		{
			if(item.contextMenuItemSelected(menuitem))
			{
				return true;
			}
		}
		
		return false;
	}
}
