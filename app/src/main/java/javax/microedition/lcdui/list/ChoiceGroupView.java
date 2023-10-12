/*
 * Copyright 2023 Yury Kharchenko
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

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

public class ChoiceGroupView extends ListView {
	public static final int MAX_EXPANDED_ITEMS = 10;
	private final int maxHeight;

	public ChoiceGroupView(Context context) {
		super(context);
		maxHeight = computeMaxHeight(context);
	}

	public ChoiceGroupView(Context context, AttributeSet attrs) {
		super(context, attrs);
		maxHeight = computeMaxHeight(context);
	}

	public ChoiceGroupView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		maxHeight = computeMaxHeight(context);
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public ChoiceGroupView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		maxHeight = computeMaxHeight(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, maxHeight | MeasureSpec.AT_MOST);
	}

	private static int computeMaxHeight(Context context) {
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(androidx.appcompat.R.attr.listPreferredItemHeightSmall, typedValue, true);
		float itemHeight = typedValue.getDimension(context.getResources().getDisplayMetrics());
		return (int) (itemHeight * MAX_EXPANDED_ITEMS);
	}
}
