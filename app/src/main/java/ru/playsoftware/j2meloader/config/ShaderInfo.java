/*
 *  Copyright 2020 Yury Kharchenko
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

package ru.playsoftware.j2meloader.config;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ShaderInfo implements Comparable<ShaderInfo>, Parcelable {
	public transient String dir;
	public String fragment;
	public String vertex;
	String name;
	String author;
	transient boolean outputResolution;
	transient boolean upscaling;
	transient Setting[] settings = new Setting[4];
	public float[] values;


	@SuppressWarnings("unused")    // used by Gson deserialization
	public ShaderInfo() {
	}

	public ShaderInfo(String name, String author) {
		this.name = name;
		this.author = author;
	}

	public ShaderInfo(Parcel in) {
		name = in.readString();
		author = in.readString();
		fragment = in.readString();
		vertex = in.readString();
		outputResolution = in.readByte() != 0;
		upscaling = in.readByte() != 0;
		settings = (Setting[]) in.readParcelableArray(getClass().getClassLoader());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ShaderInfo that = (ShaderInfo) o;
		return equalsNullable(name, that.name) &&
				equalsNullable(author, that.author) &&
				equalsNullable(fragment, that.fragment) &&
				equalsNullable(vertex, that.vertex);
	}

	@SuppressWarnings("EqualsReplaceableByObjectsCall")// ObjectsCall require API19
	boolean equalsNullable(String a, String b) {
		//noinspection StringEquality
		return (a == b) || (a != null && a.equals(b));
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + (name == null ? 0 : name.hashCode());
		result = 31 * result + (author == null ? 0 : author.hashCode());
		result = 31 * result + (fragment == null ? 0 : fragment.hashCode());
		result = 31 * result + (vertex == null ? 0 : vertex.hashCode());
		return result;
	}

	public static final Creator<ShaderInfo> CREATOR = new Creator<ShaderInfo>() {
		@Override
		public ShaderInfo createFromParcel(Parcel in) {
			return new ShaderInfo(in);
		}

		@Override
		public ShaderInfo[] newArray(int size) {
			return new ShaderInfo[size];
		}
	};

	public void set(String line) {
		int es = line.indexOf('=');
		if (es < 4) return;
		String name = line.substring(0, es++).trim();
		if (es > line.length()) return;
		String value = line.substring(es).trim();
		switch (name) {
			case "Name":
				this.name = value;
				break;
			case "Author":
				author = value;
				break;
			case "Fragment":
				fragment = value;
				break;
			case "Vertex":
				vertex = value;
				break;
			case "OutputResolution":
				outputResolution = Boolean.parseBoolean(value);
				break;
			case "Upscaling":
				upscaling = Boolean.parseBoolean(value);
				break;
			case "SettingName1":
				if (settings[0] == null) settings[0] = new Setting();
				settings[0].name = value;
				break;
			case "SettingName2":
				if (settings[1] == null) settings[1] = new Setting();
				settings[1].name = value;
				break;
			case "SettingName3":
				if (settings[2] == null) settings[2] = new Setting();
				settings[2].name = value;
				break;
			case "SettingName4":
				if (settings[3] == null) settings[3] = new Setting();
				settings[3].name = value;
				break;
			case "SettingDefaultValue1":
				if (settings[0] == null) settings[0] = new Setting();
				settings[0].def = Float.parseFloat(value);
				break;
			case "SettingDefaultValue2":
				if (settings[1] == null) settings[1] = new Setting();
				settings[1].def = Float.parseFloat(value);
				break;
			case "SettingDefaultValue3":
				if (settings[2] == null) settings[2] = new Setting();
				settings[2].def = Float.parseFloat(value);
				break;
			case "SettingDefaultValue4":
				if (settings[3] == null) settings[3] = new Setting();
				settings[3].def = Float.parseFloat(value);
				break;
			case "SettingMaxValue1":
				if (settings[0] == null) settings[0] = new Setting();
				settings[0].max = Float.parseFloat(value);
				break;
			case "SettingMaxValue2":
				if (settings[1] == null) settings[1] = new Setting();
				settings[1].max = Float.parseFloat(value);
				break;
			case "SettingMaxValue3":
				if (settings[2] == null) settings[2] = new Setting();
				settings[2].max = Float.parseFloat(value);
				break;
			case "SettingMaxValue4":
				if (settings[3] == null) settings[3] = new Setting();
				settings[3].max = Float.parseFloat(value);
				break;
			case "SettingMinValue1":
				if (settings[0] == null) settings[0] = new Setting();
				settings[0].min = Float.parseFloat(value);
				break;
			case "SettingMinValue2":
				if (settings[1] == null) settings[1] = new Setting();
				settings[1].min = Float.parseFloat(value);
				break;
			case "SettingMinValue3":
				if (settings[2] == null) settings[2] = new Setting();
				settings[2].min = Float.parseFloat(value);
				break;
			case "SettingMinValue4":
				if (settings[3] == null) settings[3] = new Setting();
				settings[3].min = Float.parseFloat(value);
				break;
			case "SettingStep1":
				if (settings[0] == null) settings[0] = new Setting();
				settings[0].step = Float.parseFloat(value);
				break;
			case "SettingStep2":
				if (settings[1] == null) settings[1] = new Setting();
				settings[1].step = Float.parseFloat(value);
				break;
			case "SettingStep3":
				if (settings[2] == null) settings[2] = new Setting();
				settings[2].step = Float.parseFloat(value);
				break;
			case "SettingStep4":
				if (settings[3] == null) settings[3] = new Setting();
				settings[3].step = Float.parseFloat(value);
				break;
		}
	}

	@NonNull
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(ShaderInfo o) {
		return name.toLowerCase().compareTo(o.name.toLowerCase());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(author);
		dest.writeString(fragment);
		dest.writeString(vertex);
		dest.writeByte((byte) (outputResolution ? 1 : 0));
		dest.writeByte((byte) (upscaling ? 1 : 0));
		dest.writeArray(settings);
	}

	static class Setting implements Parcelable {
		String name;
		float def;
		float min;
		float max;
		float step;

		protected Setting(Parcel in) {
			name = in.readString();
			def = in.readFloat();
			min = in.readFloat();
			max = in.readFloat();
			step = in.readFloat();
		}

		public Setting() {
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(name);
			dest.writeFloat(def);
			dest.writeFloat(min);
			dest.writeFloat(max);
			dest.writeFloat(step);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final Creator<Setting> CREATOR = new Creator<Setting>() {
			@Override
			public Setting createFromParcel(Parcel in) {
				return new Setting(in);
			}

			@Override
			public Setting[] newArray(int size) {
				return new Setting[size];
			}
		};
	}
}
