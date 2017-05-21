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

import android.view.LayoutInflater;
import android.view.View;

public class XMLForm extends Screen
{
	protected View view;
	protected int resID;
	
	public XMLForm(String title, int resID)
	{
		setTitle(title);
		this.resID = resID;
	}
	
	public View getScreenView()
	{
		if(view == null)
		{
			view = LayoutInflater.from(getParentActivity()).inflate(resID, null);
		}
		
		return view;
	}

	public void clearScreenView()
	{
		view = null;
	}
}