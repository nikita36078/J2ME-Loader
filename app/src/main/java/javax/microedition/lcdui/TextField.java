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

import android.content.Context;
import android.graphics.Rect;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

public class TextField extends Item
{
	public static final int ANY = 0;
	public static final int EMAILADDR = 1;
	public static final int NUMERIC = 2;
	public static final int PHONENUMBER = 3;
	public static final int URL = 4;
	public static final int DECIMAL = 5;
	public static final int CONSTRAINT_MASK = 65535;
	
	public static final int PASSWORD = 65536;
	public static final int UNEDITABLE = 131072;
	public static final int SENSITIVE = 262144;
	public static final int NON_PREDICTIVE = 524288;
	public static final int INITIAL_CAPS_WORD = 1048576;
	public static final int INITIAL_CAPS_SENTENCE = 2097152;
	
	private String text;
	private EditText textview;
	private int maxSize;
	private int constraints;
	
	private class InternalEditText extends EditText
	{
		public InternalEditText(Context context)
		{
			super(context);
		}
		
		public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
		{
			super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
			
			if(!gainFocus)
			{
				notifyStateChanged();
			}
		}
		
		public void onWindowFocusChanged(boolean hasWindowFocus)
		{
			super.onWindowFocusChanged(hasWindowFocus);
			
			if(!hasWindowFocus)
			{
				notifyStateChanged();
			}
		}
	}
	
	public TextField(String label, String text, int maxSize, int constraints)
	{
		setLabel(label);
		setMaxSize(maxSize);
		setConstraints(constraints);
		setString(text);
	}
	
	public void setString(String text)
	{
		if(text != null && text.length() > maxSize)
		{
			throw new IllegalArgumentException("text length exceeds max size");
		}
		
		this.text = text;
		
		if(textview != null)
		{
			textview.setText(text);
		}
	}
	
	public String getString()
	{
		if(textview != null)
		{
			text = textview.getText().toString();
		}
		
		return text;
	}
	
	public int size()
	{
		return getString().length();
	}
	
	public void setMaxSize(int maxSize)
	{
		if(maxSize <= 0)
		{
			throw new IllegalArgumentException("max size must be > 0");
		}
		
		this.maxSize = maxSize;
		
		if(textview != null)
		{
			textview.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxSize) });
		}
	}
	
	public int getMaxSize()
	{
		return maxSize;
	}
	
	public void setConstraints(int constraints)
	{
		this.constraints = constraints;
		
		if(textview != null)
		{
			int inputtype = 0;
			
			switch(constraints & CONSTRAINT_MASK)
			{
				default:
				case ANY:
					inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
					break;
					
				case EMAILADDR:
					inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
					break;
					
				case NUMERIC:
					inputtype = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
					break;
					
				case PHONENUMBER:
					inputtype = InputType.TYPE_CLASS_PHONE;
					break;
					
				case URL:
					inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
					break;
					
				case DECIMAL:
					inputtype = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL;
					break;
			}
			
			if((constraints & PASSWORD) != 0 ||
			   (constraints & SENSITIVE) != 0)
			{
				inputtype = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}
			
			if((constraints & UNEDITABLE) != 0)
			{
				inputtype = InputType.TYPE_NULL;
			}
			
			if((constraints & NON_PREDICTIVE) != 0)
			{
				inputtype |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			}
			
			if((constraints & INITIAL_CAPS_WORD) != 0)
			{
				inputtype |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
			}
			
			if((constraints & INITIAL_CAPS_SENTENCE) != 0)
			{
				inputtype |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
			}
			
			textview.setInputType(inputtype);
		}
	}
	
	public int getConstrants()
	{
		return constraints;
	}
	
	public View getItemContentView()
	{
		if(textview == null)
		{
			Context context = getOwnerForm().getParentActivity();
			
			textview = new InternalEditText(context);
			
			// textview.setBackgroundDrawable(Item.createBackground(context));
			// textview.setTextColor(context.getResources().getColor(android.R.color.white));
			
			setMaxSize(maxSize);
			setConstraints(constraints);
			setString(text);
		}
		
		return textview;
	}
	
	public void clearItemContentView()
	{
		textview = null;
	}

	public int getCaretPosition()
	{
		if(textview != null)
		{
			return textview.getSelectionEnd();
		}
		else
		{
			return -1;
		}
	}
}