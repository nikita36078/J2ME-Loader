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

package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface MetaDataControl extends Control {
	public static final String TRACK_NUMBER_KEY = "tracknum";
	public static final String ALBUM_KEY = "album";
	public static final String ARTIST_KEY = "artist";
	public static final String AUTHOR_KEY = "author";
	public static final String COMPOSER_KEY = "composer";
	public static final String DATE_KEY = "date";
	public static final String GENRE_KEY = "genre";
	public static final String TITLE_KEY = "title";
	public static final String YEAR_KEY = "year";
	public static final String DURATION_KEY = "duration";
	public static final String NUM_TRACKS_KEY = "numtracks";
	public static final String WRITER_KEY = "writer";
	public static final String MIME_TYPE_KEY = "mimetype";
	public static final String ALBUM_ARTIST_KEY = "albumartist";
	public static final String DISC_NUMBER_KEY = "discnum";
	public static final String COMPILATION_KEY = "compilation";
	public static final String COPYRIGHT_KEY = "copyright";

	public String[] getKeys();

	public String getKeyValue(String key);
}