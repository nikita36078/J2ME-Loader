package com.nokia.mid.sound;

public class Sound {
    public static final int FORMAT_TONE = 1;
    public static final int FORMAT_WAV = 5;
    public static final int SOUND_PLAYING = 0;
    public static final int SOUND_STOPPED = 1;
    public static final int SOUND_UNINITIALIZED = 3;

    public Sound(int i, long j) {
    }

    public Sound(byte[] bArr, int i) {
    }

    public static int getConcurrentSoundCount(int i) {
        return 0;
    }

    public static int[] getSupportedFormats() {
        return new int[0];
    }

    public int getGain() {
        return 0;
    }

    public int getState() {
        return 1;
    }

    public void init(int i, long j) {
    }

    public void init(byte[] bArr, int i) {
    }

    public synchronized void play(int i) {
    }

    public void release() {
    }

    public void resume() {
    }

    public void setGain(int i) {
    }

    public void setSoundListener(SoundListener soundListener) {
    }

    public void stop() {
    }
}
