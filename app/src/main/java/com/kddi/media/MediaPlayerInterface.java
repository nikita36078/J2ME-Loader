/*
 * Copyright 2021 ohayoyogi
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

package com.kddi.media;

public interface MediaPlayerInterface {

    void addMediaEventListener(MediaEventListener l);

    int getAttribute(int attr);

    int getPitch();

    MediaResource getResource();

    int getTempo();

    int getVolume();

    @Deprecated
    void hide();

    void pause();

    void play();

    void play(int count);

    void removeMediaEventListener(MediaEventListener l);

    void resume();

    void setAttribute(int attr, int value);

    void setPitch(int pitch);

    void setResource(MediaResource resource);

    void setTemp(int tempo);

    void setVolume(int volume);

    @Deprecated
    void show();

    void stop();

    void unsetResource(MediaResource resource);
}
