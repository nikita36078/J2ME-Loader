/*
 *  Copyright 2023 Yury Kharchenko
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

package ru.woesss.j2me.mmapi.tsf;

class TinySoundFont {
	static native void init(String soundBank);
	static native long playerInit(String locator);
	static native void playerFinalize(long handle);
	static native void playerRealize(long handle);
	static native void playerPrefetch(long handle);
	static native void playerStart(long handle);
	static native void playerPause(long handle);
	static native void playerDeallocate(long handle);
	static native void playerClose(long handle);
	static native long setMediaTime(long handle, long now);
	static native long getMediaTime(long handle);
	static native void setRepeat(long handle, int count);
	static native int setPan(long handle, int pan);
	static native int getPan(long handle);
	static native void setMute(long handle, boolean mute);
	static native boolean isMuted(long handle);
	static native int setVolume(long handle, int level);
	static native int getVolume(long handle);
	static native long playerGetDuration(long handle);
	static native void playerListener(long handle, PlayerTSF listener);

	static {
		System.loadLibrary("c++_shared");
		System.loadLibrary("oboe");
		System.loadLibrary("mmapi_common");
		System.loadLibrary("mmapi_tsf");
	}
}
