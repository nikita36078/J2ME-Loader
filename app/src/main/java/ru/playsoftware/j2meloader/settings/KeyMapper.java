/*
 * Copyright 2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.microedition.lcdui.Canvas;

public class KeyMapper {
	private static final String PREF_KEY = "pref_key_mapping";

	public static void saveArrayPref(Context context, SparseIntArray intArray) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		JSONArray json = new JSONArray();
		StringBuilder  data = new StringBuilder().append("[");
		for (int i = 0; i < intArray.size(); i++) {
			data.append("{")
					.append("\"key\": ")
					.append(intArray.keyAt(i)).append(",")
					.append("\"value\": ")
					.append(intArray.valueAt(i))
					.append("},");
			json.put(data);
		}
		data.deleteCharAt(data.length() - 1);
		data.append("]");
		editor.putString(PREF_KEY, intArray.size() == 0 ? null : data.toString());
		editor.apply();
	}

	public static SparseIntArray getArrayPref(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String json = prefs.getString(PREF_KEY, null);
		SparseIntArray intArray = new SparseIntArray();
		if (json != null) {
			try {
				JSONArray jsonArray = new JSONArray(json);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject item = jsonArray.getJSONObject(i);
					intArray.put(item.getInt("key"), item.getInt("value"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			initArray(intArray);
		}
		return intArray;
	}

	public static void initArray(SparseIntArray intDict) {
		intDict.put(KeyEvent.KEYCODE_0, Canvas.KEY_NUM0);
		intDict.put(KeyEvent.KEYCODE_1, Canvas.KEY_NUM1);
		intDict.put(KeyEvent.KEYCODE_2, Canvas.KEY_NUM2);
		intDict.put(KeyEvent.KEYCODE_3, Canvas.KEY_NUM3);
		intDict.put(KeyEvent.KEYCODE_4, Canvas.KEY_NUM4);
		intDict.put(KeyEvent.KEYCODE_5, Canvas.KEY_NUM5);
		intDict.put(KeyEvent.KEYCODE_6, Canvas.KEY_NUM6);
		intDict.put(KeyEvent.KEYCODE_7, Canvas.KEY_NUM7);
		intDict.put(KeyEvent.KEYCODE_8, Canvas.KEY_NUM8);
		intDict.put(KeyEvent.KEYCODE_9, Canvas.KEY_NUM9);
		intDict.put(KeyEvent.KEYCODE_STAR, Canvas.KEY_STAR);
		intDict.put(KeyEvent.KEYCODE_POUND, Canvas.KEY_POUND);
		intDict.put(KeyEvent.KEYCODE_DPAD_UP, Canvas.KEY_UP);
		intDict.put(KeyEvent.KEYCODE_DPAD_DOWN, Canvas.KEY_DOWN);
		intDict.put(KeyEvent.KEYCODE_DPAD_LEFT, Canvas.KEY_LEFT);
		intDict.put(KeyEvent.KEYCODE_DPAD_RIGHT, Canvas.KEY_RIGHT);
		intDict.put(KeyEvent.KEYCODE_ENTER, Canvas.KEY_FIRE);
		intDict.put(KeyEvent.KEYCODE_SOFT_LEFT, Canvas.KEY_SOFT_LEFT);
		intDict.put(KeyEvent.KEYCODE_SOFT_RIGHT, Canvas.KEY_SOFT_RIGHT);
		intDict.put(KeyEvent.KEYCODE_CALL, Canvas.KEY_SEND);
		intDict.put(KeyEvent.KEYCODE_ENDCALL, Canvas.KEY_END);
	}
}
