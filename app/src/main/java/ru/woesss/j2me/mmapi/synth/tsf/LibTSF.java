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

class LibTSF {
	static native void loadSoundBank(String soundBank);
	static native long createPlayer(String locator);
	static native void finalize(long handle);
	static native void realize(long handle);
	static native void prefetch(long handle);
	static native void start(long handle);
	static native void pause(long handle);
	static native void deallocate(long handle);
	static native void close(long handle);
	static native long setMediaTime(long handle, long now);
	static native long getMediaTime(long handle);
	static native void setRepeat(long handle, int count);
	static native int setPan(long handle, int pan);
	static native int getPan(long handle);
	static native void setMute(long handle, boolean mute);
	static native boolean isMuted(long handle);
	static native int setVolume(long handle, int level);
	static native int getVolume(long handle);
	static native long getDuration(long handle);
	static native void setListener(long handle, Object listener);
	static native void setDataSource(long handle, byte[] data);

	static {
		System.loadLibrary("c++_shared");
		System.loadLibrary("oboe");
		System.loadLibrary("mmapi_common");
		System.loadLibrary("mmapi_tsf");
	}
}
