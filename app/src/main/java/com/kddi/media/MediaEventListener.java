package com.kddi.media;

public interface MediaEventListener {
    void stateChanged(MediaPlayerBox source, int type, int option);
}
