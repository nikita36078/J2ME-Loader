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

package com.jblend.media;

import com.jblend.io.j2me.events.NativeMediaEventDispatcher;

public abstract class MediaPlayer {
	public static final int NO_DATA = 0;
	public static final int READY = 1;
	public static final int PLAYING = 2;
	public static final int PAUSED = 3;
	public static final int ERROR = 65536;
	protected static final int REAL_WIDTH = 0;
	protected static final int REAL_HEIGHT = 0;

	public MediaPlayer() {
	}

	public abstract void setData(MediaData data);

	public abstract void play();

	public abstract void play(boolean isRepeat);

	public abstract void play(int count);

	public abstract void stop();

	public abstract void pause();

	public abstract void resume();

	public abstract int getState();

	public abstract void addMediaPlayerListener(MediaPlayerListener l);

	public abstract void removeMediaPlayerListener(MediaPlayerListener l);

	protected static void addNativeMediaEventDispatcher(NativeMediaEventDispatcher dispatcher) {
	}
}
