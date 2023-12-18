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

package com.vodafone.media.audio3d;

/** @noinspection unused*/
public interface Audio3DControl extends ExtendedAudioControl {
	int MODE_DYNAMIC = 2;

	int[] getPosition();
	int[] getRolloff();
	int[] getVelocity();
	boolean isListenerRelative();
	void setListenerRelative(boolean b);
	void setPosition(int x, int y, int z);
	void setRolloff(int x, int y, int z);
	void setVelocity(int x, int y, int z);
}
