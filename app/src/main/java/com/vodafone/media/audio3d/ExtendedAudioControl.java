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

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface ExtendedAudioControl extends Control {
 /** @noinspection unused*/
 int MODE_DISABLED = 0;
 /** @noinspection unused*/
 int MODE_EXTENDED = 1;

 int getMode();
 void setMode(int paramInt) throws MediaException;
}
