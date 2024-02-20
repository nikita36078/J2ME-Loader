/*
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

package ru.woesss.j2me.mmapi.synth.tsf;

import ru.woesss.j2me.mmapi.synth.Library;

public class LibTSF implements Library {

	public LibTSF(String soundBank) {
		loadSoundBank(soundBank);
	}

	@Override
	public native void loadSoundBank(String soundBank);
	@Override
	public native long createPlayer(String locator);
	@Override
	public native void finalize(long handle);
	@Override
	public native void realize(long handle);
	@Override
	public native void prefetch(long handle);
	@Override
	public native void start(long handle);
	@Override
	public native void pause(long handle);
	@Override
	public native void deallocate(long handle);
	@Override
	public native void close(long handle);
	@Override
	public native long setMediaTime(long handle, long now);
	@Override
	public native long getMediaTime(long handle);
	@Override
	public native void setRepeat(long handle, int count);
	@Override
	public native void setPan(long handle, int pan);
	@Override
	public native void setVolume(long handle, int level);
	@Override
	public native long getDuration(long handle);
	@Override
	public native void setListener(long handle, Object listener);
	@Override
	public native void setDataSource(long handle, byte[] data);
	@Override
	public int writeMIDI(long handle, byte[] data, int offset, int length) {
		return 0;
	}

	@Override
	public boolean hasToneControl() {
		return false;
	}

	static {
		System.loadLibrary("c++_shared");
		System.loadLibrary("oboe");
		System.loadLibrary("mmapi_common");
		System.loadLibrary("mmapi_tsf");
	}
}
