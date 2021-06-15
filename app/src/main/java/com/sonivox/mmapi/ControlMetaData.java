package com.sonivox.mmapi;

import java.util.*;

import javax.microedition.media.control.MetaDataControl;

/*
 * TODO: for full TCK compatibility, this class should only be returned for
 * Players that actuallay have at least one meta data entry.
 */

/**
 * Implementation of meta data control for EAS.
 */
class ControlMetaData extends ControlBase implements MetaDataControl {

	/**
	 * The collection of meta data
	 */
	private Hashtable data; // = null;

	/**
	 * Create a new instance of this EAS meta data control.
	 * 
	 * @param player the owning player
	 */
	ControlMetaData(PlayerEAS player) {
		super(player);
	}

	private String metaDataConstantToKey(int constant) {
		switch (constant) {
		case EAS.METADATA_TITLE:
			return TITLE_KEY;
		case EAS.METADATA_AUTHOR:
			return AUTHOR_KEY;
		case EAS.METADATA_COPYRIGHT:
			return COPYRIGHT_KEY;
		case EAS.METADATA_LYRIC:
			return "lyric";
		}
		return null;
	}

	/**
	 * Handle meta data. This method adds all meta data that the native layer
	 * has gathered to the supplied hash table.
	 */
	private void gatherMetaData() {
		int nextType;
		do {
			nextType = EAS.getNextMetaDataType(player.handle);
			if (nextType >= 0) {
				String value = EAS.getNextMetaDataValue(player.handle);
				String key = metaDataConstantToKey(nextType);
				if (key != null && key != "" && value != null && value != "") {
					// add this key/value pair to the hash table
					if (data == null) {
						data = new Hashtable(4);
					}
					data.put(key, value);
				}
			}
		} while (nextType >= 0);

		// we may have an updated duration now, so let Player try to get it
		player.calcDuration(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MetaDataControl#getKeys()
	 */
	public String[] getKeys() {
		gatherMetaData();
		if (data == null) {
			return new String[0];
		}
		String[] result = new String[data.size()];
		Enumeration keys = data.keys();
		int i = 0;
		while (keys.hasMoreElements()) {
			result[i++] = (String) keys.nextElement();
		}
		return result;
	}

	/**
	 * This method is specified to only return null for those keys that will
	 * eventually yield meta data. But how would the implementation know which
	 * meta data types will be available when it doesn't know the values? The
	 * specification puts too much burden on the implementation.
	 * 
	 * @see MetaDataControl#getKeyValue(String)
	 */
	public String getKeyValue(String key) {
		if (key == null) {
			throw new IllegalArgumentException("key is null");
		}
		// check validity
		boolean valid = false;
		for (int i = 0; i < EAS.METADATA_LAST; i++) {
			if (key.equals(metaDataConstantToKey(i))) {
				valid = true;
				break;
			}
		}
		if (!valid) {
			throw new IllegalArgumentException("invalid key");
		}
		gatherMetaData();
		if (data == null) {
			return null;
		}
		return (String) data.get(key);
	}

}
