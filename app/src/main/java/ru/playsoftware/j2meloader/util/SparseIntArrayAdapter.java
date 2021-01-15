package ru.playsoftware.j2meloader.util;

import android.util.SparseIntArray;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

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
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		in.beginObject();
		SparseIntArray array = new SparseIntArray();
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
