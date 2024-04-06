/*
 * Copyright 2024 Yury Kharchenko
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

package com.j_phone.io;

import com.j_phone.phonedata.MailData;
import com.jblend.media.MediaPlayer;
import com.jblend.media.MediaData;

import java.io.IOException;

public final class FileUtility {
	public static final int COUNT_LIMIT = 3;
	public static final int EXISTS = 1;
	public static final int FILETYPE_DIFFERENT = 4;
	public static final int INSUFFICIENT = 2;
	public static final int OTHER_ERROR = -1;
	public static final int WRITABLE = 0;
	public static final int WRITE_PROTECT = 5;

	public static FileUtility getInstance() {
		return null;
	}

	public void play(String path) throws IOException {
	}

	public void play(MailData mailData, int attachedFileIndex) throws IOException {
	}

	public void play(byte[] data, int type) throws IOException {
	}

	public MediaPlayer getMediaPlayer(String path) throws IOException {
		return null;
	}

	public MediaPlayer getMediaPlayer(String path, int type) throws IOException {
		return null;
	}

	public MediaData getMediaData(String path) throws IOException {
		return null;
	}

	public MediaData getMediaData(String path, int type) throws IOException {
		return null;
	}

	public int getFreeSpace(String rootpath) throws IOException {
		return 0;
	}

	public int precheckStorable(String path, int size) {
		return 0;
	}
}
