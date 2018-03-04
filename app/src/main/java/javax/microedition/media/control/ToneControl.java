/*
 * Copyright 2018 Nikita Shakarun
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

package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface ToneControl extends Control {
	public static final byte VERSION = -2;

	public static final byte TEMPO = -3;

	public static final byte RESOLUTION = -4;

	public static final byte BLOCK_START = -5;

	public static final byte BLOCK_END = -6;

	public static final byte PLAY_BLOCK = -7;

	public static final byte SET_VOLUME = -8;

	public static final byte REPEAT = -9;

	public static final byte C4 = 60;

	public static final byte SILENCE = -1;

	public void setSequence(byte[] sequence);
}
