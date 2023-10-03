/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2019 Nikita Shakarun
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

package javax.microedition.media;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.collection.SparseArrayCompat;

import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.protocol.DataSource;

public class InternalMetaData implements MetaDataControl {
	private static final String TAG = InternalMetaData.class.getSimpleName();

	private static final ArrayList<Integer> androidMetaKeys = new ArrayList<>();
	private static final SparseArrayCompat<String> androidMetaToMIDP = new SparseArrayCompat<>();

	private final ArrayList<String> metaKeys = new ArrayList<>();
	private final HashMap<String, String> metaData = new HashMap<>();

	private static void mapMetaKey(int android, String midp) {
		androidMetaKeys.add(android);
		androidMetaToMIDP.put(android, midp);
	}

	private void updateMetaData(MediaMetadataRetriever retriever) {
		metaKeys.clear();
		metaData.clear();

		String key, value;

		for (Integer keyCode : androidMetaKeys) {
			value = retriever.extractMetadata(keyCode);

			if (value != null) {
				key = androidMetaToMIDP.get(keyCode);

				metaKeys.add(key);
				metaData.put(key, value);
			}
		}
	}

	@Override
	public String[] getKeys() {
		return metaKeys.toArray(new String[0]);
	}

	@Override
	public String getKeyValue(String key) {
		return metaData.get(key);
	}

	public void updateMetaData(DataSource source) {
		try {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			retriever.setDataSource(source.getLocator());
			updateMetaData(retriever);
			retriever.release();
		} catch (Exception e) {
			Log.w(TAG, "updateMetaData: ", e);
		}
	}

	static {
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, TRACK_NUMBER_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_ALBUM, ALBUM_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_ARTIST, ARTIST_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_AUTHOR, AUTHOR_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_COMPOSER, COMPOSER_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_DATE, DATE_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_GENRE, GENRE_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_TITLE, TITLE_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_YEAR, YEAR_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_DURATION, DURATION_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS, NUM_TRACKS_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_WRITER, WRITER_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_MIMETYPE, MIME_TYPE_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, ALBUM_ARTIST_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, DISC_NUMBER_KEY);
		mapMetaKey(MediaMetadataRetriever.METADATA_KEY_COMPILATION, COMPILATION_KEY);
	}
}