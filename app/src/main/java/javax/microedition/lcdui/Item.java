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

import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.SimpleEvent;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class Item implements View.OnCreateContextMenuListener
{
	public static final int PLAIN = 0;
	public static final int HYPERLINK = 1;
	public static final int BUTTON = 2;
	
	public static final int LAYOUT_DEFAULT = 0;
	public static final int LAYOUT_LEFT = 1;
	public static final int LAYOUT_RIGHT = 2;
	public static final int LAYOUT_CENTER = 3;
	public static final int LAYOUT_TOP = 16;
	public static final int LAYOUT_BOTTOM = 32;
	public static final int LAYOUT_VCENTER = 48;
	public static final int LAYOUT_NEWLINE_BEFORE = 256;
	public static final int LAYOUT_NEWLINE_AFTER = 512;
	public static final int LAYOUT_SHRINK = 1024;
	public static final int LAYOUT_EXPAND = 2048;
	public static final int LAYOUT_VSHRINK = 4096;
	public static final int LAYOUT_VEXPAND = 8192;
	public static final int LAYOUT_2 = 16384;
	
	private static final float BORDER_PADDING = 7;
	private static final float BORDER_RADIUS = 4;
	
	private static final int LABEL_NO_ACTION = 0;
	private static final int LABEL_SHOW = 1;
	private static final int LABEL_HIDE = 2;
	
	private LinearLayout layout;
	private View contentview;
	
	private String label;
	private TextView labelview;
	private int labelmode;
	
	private Form owner;
	
	private ArrayList<Command> commands = new ArrayList();
	private ItemCommandListener listener = null;
	
	private SimpleEvent msgSetContextMenuListener = new SimpleEvent()
	{
		public void process()
		{
			if(listener != null)
			{
				layout.setOnCreateContextMenuListener(Item.this);
				labelview.setOnCreateContextMenuListener(Item.this);
				contentview.setOnCreateContextMenuListener(Item.this);
			}
			else
			{
				layout.setLongClickable(false);
				labelview.setLongClickable(false);
				contentview.setLongClickable(false);
			}
		}
	};
	
	private SimpleEvent msgSetLabel = new SimpleEvent()
	{
		public void process()
		{
//			System.out.println("Changing label from " + Thread.currentThread());
			
			labelview.setText(label);
			
			switch(labelmode)
			{
				case LABEL_SHOW:
					layout.addView(labelview, 0);
					break;
					
				case LABEL_HIDE:
					layout.removeView(labelview);
					break;
			}
			
			labelmode = LABEL_NO_ACTION;
		}
	};
	
	public void setLabel(String value)
	{
		if(layout != null)
		{
			if(label == null && value != null)
			{
				labelmode = LABEL_SHOW;
			}
			else if(label != null && value == null)
			{
				labelmode = LABEL_HIDE;
			}
			
			label = value;
			
			ViewHandler.postEvent(msgSetLabel);
		}
		else
		{
			label = value;
		}
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public void setOwnerForm(Form form)
	{
		owner = form;
		clearItemView();
	}
	
	public Form getOwnerForm()
	{
		if(owner == null)
		{
			throw new IllegalStateException("call setOwnerForm() before calling getOwnerForm()");
		}
		
		return owner;
	}
	
	public boolean hasOwnerForm()
	{
		return owner != null;
	}
	
	public void notifyStateChanged()
	{
		if(owner != null)
		{
			owner.notifyItemStateChanged(this);
		}
	}
	
	public void setLayout(int value)
	{
	}
	
	public static Drawable createBackground(Context context, int color)
	{
		float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BORDER_RADIUS, context.getResources().getDisplayMetrics());
		float[] radii = new float[8];
		
		for(int i = 0; i < 8; i++)
		{
			radii[i] = radius;
		}
		
		ShapeDrawable drawable = new ShapeDrawable(new RoundRectShape(radii, null, null));
		Paint paint = drawable.getPaint();
		
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2);
		
		int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BORDER_PADDING, context.getResources().getDisplayMetrics()));
		drawable.setPadding(padding, padding, padding, padding);
		
		return drawable;
	}
	
	/**
	 * Получить весь элемент, то есть
	 * @return LinearLayout с меткой в первом ряду и некоторым содержимым во втором
	 */
	public View getItemView()
	{
		if(layout == null)
		{
			Context context = owner.getParentActivity();
			
			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			
			int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BORDER_PADDING, context.getResources().getDisplayMetrics()));
			layout.setPadding(padding, padding, padding, padding);
			
			labelview = new TextView(context);
			labelview.setTextAppearance(context, android.R.style.TextAppearance_Medium);
			labelview.setText(label);
			
			if(label != null)
			{
				layout.addView(labelview, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			}
			
			contentview = getItemContentView();
			layout.addView(contentview);
			
			ViewHandler.postEvent(msgSetContextMenuListener);
		}
		
		return layout;
	}
	
	public void clearItemView()
	{
		layout = null;
		labelview = null;
		contentview = null;
		
		clearItemContentView();
	}
	
	/**
	 * Получить только содержимое элемента.
	 */
	protected abstract View getItemContentView();
	protected abstract void clearItemContentView();
	
	public void addCommand(Command cmd)
	{
		commands.add(cmd);
	}
	
	public void removeCommand(Command cmd)
	{
		commands.remove(cmd);
	}

	public void setDefaultCommand(Command cmd) {}
	
	public void removeAllCommands()
	{
		commands.clear();
	}
	
	public void setItemCommandListener(ItemCommandListener listener)
	{
		this.listener = listener;
		
		if(layout != null)
		{
			ViewHandler.postEvent(msgSetContextMenuListener);
		}
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.clear();
		
		for(Command cmd : commands)
		{
			menu.add(Menu.NONE, cmd.hashCode(), cmd.getPriority(), cmd.getLabel());
		}
	}
	
	public boolean contextMenuItemSelected(MenuItem item)
	{
		if(listener == null)
		{
			return false;
		}
		
		int id = item.getItemId();
		
		for(Command cmd : commands)
		{
			if(cmd.hashCode() == id)
			{
				if(owner != null)
				{
					owner.postEvent(CommandActionEvent.getInstance(listener, cmd, this));
				}
				
				return true;
			}
		}
		
		return false;
	}
}