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

package com.nokia.mid.sound;

import android.util.Log;

final class ToneVerifier {
	private static final String TAG = ToneVerifier.class.getSimpleName();
	private final byte[] data;
	private int pos;

	private ToneVerifier(byte[] tone) {
		data = tone;
	}

	static void fix(byte[] tone) {
		ToneVerifier instance = new ToneVerifier(tone);
		try {
			instance.parseTone();
		} catch (Exception e) {
			Log.e(TAG, "Error parsing tone", e);
		}
	}

	private int read(int length) {
		int p = pos / 8;
		int bit = pos % 8;
		int d = (data[p] & 255) << 8;
		if (bit + length > 8)
			d += data[p + 1] & 255;
		pos += length;
		return d >> 16 - bit - length & (1 << length) - 1;
	}

	private void skip(int length) {
		pos += length;
	}

	private void replace8(byte[] data, int offset, int value) {
		int p = offset / 8;
		int bit = offset % 8;
		if (bit == 0) {
			data[p] = (byte) value;
			return;
		}
		data[p] = (byte) (((data[p] >> 8 - bit) << 8 - bit) | (value >> bit));
		data[p + 1] = (byte) (((data[p + 1] << 24 + bit) >>> 24 + bit) |
				((value << 32 - bit) >>> 24));
	}

	// TODO: 13.03.2021 does not fully support format specification.
	private void parseTone() {
		int charWidth;
		skip(8);// command length
		skip(8);//<ringing-tone-programming> and filler bit
		int partId = read(7);
		if (partId == 0b0100_010) {//<unicode>
			charWidth = 16;
			read(1);
			skip(7); //<sound>
		} else {
			charWidth = 8;
		}
		// song type
		int songType = read(3);
		if (songType == 1) {
			int len = read(4); // title length
			for (int i = 0; i < len; i++) {
				skip(charWidth); // title char
			}
		} else if (songType != 2) {
			Log.e(TAG, "Unsupported ringtone type");
			return;
		}

		// sequence length
		label:for (int j = read(8); j > 0; --j) {
			skip(3); //<pattern-header-id>
			skip(2); //<pattern-id>
			skip(4); //<loop-value>
			int patSpecOffset = this.pos;
			int len = read(8); //<pattern-specifier> (length)

			for (int k = 0; k < len; k++) {
				int id = read(3);
				switch (id) {
					case 1: //<note-instruction-id>
						skip(4);    //<note-value>
						skip(3);    //<note-duration>
						skip(2);    //<note-duration-specifier>
						break;
					case 2: //<scale-instruction-id>
						skip(2);    //<note-scale>
						break;
					case 3: //<style-instruction-id>
						skip(2);    //<style-value>
						break;
					case 4: //<tempo-instruction-id>
						skip(5);    //<beats-per-minute>
						break;
					case 5: //<volume-instruction-id>
						skip(4);    //<volume>
						break;
					default:
						Log.e(TAG, "Unexpected instruction: " + id);
						// fix instruction count ("New Skool Skater" bug)
						replace8(this.data, patSpecOffset, k);
						break label;
				}
			}
		}
	}
}
