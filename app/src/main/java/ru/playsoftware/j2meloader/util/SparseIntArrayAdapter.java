/*
 *  Copyright 2021 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.playsoftware.j2meloader.util;

import android.util.SparseIntArray;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SparseIntArrayAdapter extends TypeAdapter<SparseIntArray> {
	@Override
	public void write(JsonWriter out, SparseIntArray array) throws IOException {
		out.beginObject();
		for (int i = 0; i < array.size(); i++) {
			out.name(Integer.toString(array.keyAt(i))).value(array.valueAt(i));
		}
		out.endObject();
	}

	@Override
	public SparseIntArray read(JsonReader in) throws IOException {
		JsonToken peek = in.peek();
		if (peek == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		SparseIntArray array = new SparseIntArray();
		if (peek == JsonToken.STRING) {
			String s = in.nextString();
			try {
				JSONArray jsonArray = new JSONArray(s);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject item = jsonArray.getJSONObject(i);
					array.put(item.getInt("key"), item.getInt("value"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return array;
		}
		in.beginObject();
		while (in.hasNext()) {
			String name = in.nextName();
			int key = Integer.parseInt(name);
			int value = in.nextInt();
			array.put(key, value);
		}
		in.endObject();
		return array;
	}
}
