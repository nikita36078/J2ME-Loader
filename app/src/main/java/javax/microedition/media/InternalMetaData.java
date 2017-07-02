package javax.microedition.media;

import android.media.MediaMetadataRetriever;

import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.media.control.MetaDataControl;

public class InternalMetaData implements MetaDataControl {
	protected static ArrayList<Integer> androidMetaKeys;
	protected static HashMap<Integer, String> androidMetaToMIDP;

	static {
		androidMetaKeys = new ArrayList();
		androidMetaToMIDP = new HashMap();

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

	protected static void mapMetaKey(int android, String midp) {
		androidMetaKeys.add(android);
		androidMetaToMIDP.put(android, midp);
	}

	protected ArrayList<String> metakeys;
	protected HashMap<String, String> metadata;

	public InternalMetaData() {
		metakeys = new ArrayList();
		metadata = new HashMap();
	}

	public void updateMetaData(MediaMetadataRetriever retriever) {
		metakeys.clear();
		metadata.clear();

		String key, value;

		for (Integer keyCode : androidMetaKeys) {
			value = retriever.extractMetadata(keyCode);

			if (value != null) {
				key = androidMetaToMIDP.get(keyCode);

				metakeys.add(key);
				metadata.put(key, value);
			}
		}
	}

	public String[] getKeys() {
		return metakeys.toArray(new String[0]);
	}

	public String getKeyValue(String key) {
		return metadata.get(key);
	}
}