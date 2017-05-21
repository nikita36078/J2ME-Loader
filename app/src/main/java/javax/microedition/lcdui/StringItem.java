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

import javax.microedition.lcdui.event.SimpleEvent;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class StringItem extends Item
{
	private String text;
	private TextView textview;
	
	private SimpleEvent msgSetText = new SimpleEvent()
	{
		public void process()
		{
			textview.setText(text);
		}
	};
	
	public StringItem(String label, String text)
	{
		this(label, text, PLAIN);
	}
	
	public StringItem(String label, String text, int appearanceMode)
	{
		setLabel(label);
		setText(text);
	}
	
	public void setText(String text)
	{
		this.text = text;
		
		if(textview != null)
		{
			ViewHandler.postEvent(msgSetText);
		}
	}
	
	public String getText()
	{
		return text;
	}
	
	public View getItemContentView()
	{
		if(textview == null)
		{
			Context context = getOwnerForm().getParentActivity();
			
			textview = new TextView(context);
			textview.setTextAppearance(context, android.R.style.TextAppearance_Small);
			textview.setText(text);
		}
		
		return textview;
	}
	
	public void clearItemContentView()
	{
		textview = null;
	}
}