/*
 * Copyright 2015-2016 Nickolay Savchenko
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

package ru.playsoftware.j2meloader.filelist;

public class FSItem implements SortItem {

	private int imageId;
	Type type;
	private String name, descr;

	public FSItem(int imageId, String name, String descr, Type type) {
		this.imageId = imageId;
		this.name = name;
		this.descr = descr;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setName(String header) {
		this.name = header;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return descr;
	}

	public int getImageId() {
		return imageId;
	}

	@Override
	public String getSortField() {
		return name;
	}

	public enum Type {

		Folder, File, Back
	}
}
