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
