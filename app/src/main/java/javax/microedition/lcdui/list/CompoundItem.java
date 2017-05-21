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

import javax.microedition.lcdui.Image;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class CompoundItem
{
	protected String stringPart;
	
	protected Image imagePart;
	protected Drawable imageDrawable;
	
	public CompoundItem(String stringPart, Image imagePart)
	{
		this.stringPart = stringPart;
		this.imagePart = imagePart;
	}
	
	public void setString(String stringPart)
	{
		this.stringPart = stringPart;
	}
	
	public String getString()
	{
		return stringPart;
	}
	
	public void setImage(Image imagePart)
	{
		this.imagePart = imagePart;
		this.imageDrawable = new BitmapDrawable(imagePart.getBitmap());
	}

	public Image getImage()
	{
		return imagePart;
	}

	public Drawable getDrawable()
	{
		return imageDrawable;
	}
}