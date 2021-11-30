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

import java.util.ArrayList;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class MediaPlayerBox extends Canvas implements MediaPlayerInterface {

    public static final int RESOURCE_DISPOSED = 0;
    public static final int PLAY = 1;
    public static final int STOP = 2;
    public static final int PAUSE = 3;
    public static final int RESUME = 4;
    public static final int FOREGROUND = 5;
    public static final int BACKGROUND = 6;

    int state = STOP;

    private ArrayList<MediaEventListener> listeners;

    MediaResource resource;

    public MediaPlayerBox() {
        super();
    }

    public MediaPlayerBox(int flag) {
        this();
    }

    public MediaPlayerBox(MediaResource resource, int flag) {
        this();
        setResource(resource);
    }

    @Override
    public void addMediaEventListener(MediaEventListener l) {
        if (!listeners.contains(l) && listeners != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeMediaEventListener(MediaEventListener l) {
        listeners.remove(l);
    }

    private void postEvent(int event) {
        if (listeners != null) {
            for (MediaEventListener l : listeners
            ) {
                l.stateChanged(this, event, 0);
            }
        }
    }

    @Override
    public int getAttribute(int attr) {
        return 0;
    }

    @Override
    public int getPitch() {
        return 0;
    }

    @Override
    public MediaResource getResource() {
        return this.resource;
    }

    @Override
    public int getTempo() {
        return 0;
    }

    @Override
    public int getVolume() {
        return 0;
    }

    @Override
    public void hide() {

    }

    @Override
    public void play() {
        this.play(1);
    }

    @Override
    public void play(int count) {
        if (resource != null) {
            Player player = resource._getPlayer();

            if (player != null)
                try {
                    if (state != PLAY) {
                        player.setLoopCount(count);
                        player.start();
                        state = PLAY;
                    }
                } catch (MediaException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    public void stop() {
        if (resource != null) {
            Player player = resource._getPlayer();

            if (player != null)
                try {
                    player.stop();
                    state = STOP;
                } catch (MediaException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void setAttribute(int attr, int value) {

    }

    @Override
    public void setPitch(int pitch) {

    }

    @Override
    public void setResource(MediaResource resource) {
        if (state != STOP) throw new IllegalStateException();
        if (resource == null) throw new NullPointerException();
        this.resource = resource;

    }

    @Override
    public void setTemp(int tempo) {

    }

    @Override
    public void setVolume(int volume) {

    }

    @Override
    public void show() {

    }

    @Override
    public void unsetResource(MediaResource resource) {
        this.resource = null;
    }

    @Override
    protected void paint(Graphics g) {

    }
}
