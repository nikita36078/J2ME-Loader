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

package ru.woesss.j2me.mmapi.synth;

public interface Library {
	void loadSoundBank(String soundBank);
	long createPlayer(String locator);
	void finalize(long handle);
	void realize(long handle);
	void prefetch(long handle);
	void start(long handle);
	void pause(long handle);
	void deallocate(long handle);
	void close(long handle);
	long setMediaTime(long handle, long now);
	long getMediaTime(long handle);
	void setRepeat(long handle, int count);
	int setPan(long handle, int pan);
	int getPan(long handle);
	void setMute(long handle, boolean mute);
	boolean isMuted(long handle);
	int setVolume(long handle, int level);
	int getVolume(long handle);
	long getDuration(long handle);
	void setListener(long handle, Object listener);
	void setDataSource(long handle, byte[] data);

	boolean hasToneControl();
}
