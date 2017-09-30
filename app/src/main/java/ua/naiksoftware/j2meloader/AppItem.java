/*
 * J2ME Loader
 * Copyright (C) 2015-2016 Nickolay Savchenko
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ua.naiksoftware.j2meloader;

import java.io.Serializable;

public class AppItem implements Serializable {

	private String imagePath;
	private String title;
	private String author;
	private String version;
	private String path;

	public AppItem(String imagePath_, String title_, String author_, String version_) {
		imagePath = imagePath_;
		title = title_;
		author = author_;
		version = version_;
	}

	public void setPath(String p) {
		path = p;
		setImagePath();
	}

	public String getPath() {
		return path;
	}

	public void setTitle(String title_) {
		title = title_;
	}

	public String getTitle() {
		return title;
	}

	public void setImagePath() {
		String resString = "/res";
		imagePath = imagePath.replace(" ", "");
		if (!imagePath.contains("/")) {
			resString += "/";
		}
		imagePath = path + resString + imagePath;
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getAuthor() {
		return author;
	}

	public String getVersion() {
		return version;
	}

}
