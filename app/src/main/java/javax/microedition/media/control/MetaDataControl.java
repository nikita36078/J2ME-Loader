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
	String TRACK_NUMBER_KEY = "tracknum";
	String ALBUM_KEY = "album";
	String ARTIST_KEY = "artist";
	String AUTHOR_KEY = "author";
	String COMPOSER_KEY = "composer";
	String DATE_KEY = "date";
	String GENRE_KEY = "genre";
	String TITLE_KEY = "title";
	String YEAR_KEY = "year";
	String DURATION_KEY = "duration";
	String NUM_TRACKS_KEY = "numtracks";
	String WRITER_KEY = "writer";
	String MIME_TYPE_KEY = "mimetype";
	String ALBUM_ARTIST_KEY = "albumartist";
	String DISC_NUMBER_KEY = "discnum";
	String COMPILATION_KEY = "compilation";
	String COPYRIGHT_KEY = "copyright";

	String[] getKeys();

	String getKeyValue(String key);
}
