/*
 * Copyright 2023-2025 Yury Kharchenko
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

import static javax.microedition.media.Manager.MIDI_DEVICE_LOCATOR;
import static javax.microedition.media.Manager.TONE_DEVICE_LOCATOR;

import android.util.Log;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.amms.control.PanControl;
import javax.microedition.amms.control.audioeffect.EqualizerControl;
import javax.microedition.media.BasePlayer2;
import javax.microedition.media.Control;
import javax.microedition.media.InternalEqualizer;
import javax.microedition.media.InternalMetaData;
import javax.microedition.media.MediaException;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.MIDIControl;
import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.tone.ToneSequence;

import ru.woesss.j2me.mmapi.control.MIDIControlImpl;
import ru.woesss.j2me.mmapi.protocol.device.DeviceMetaData;

class SynthPlayer extends BasePlayer2 implements VolumeControl, PanControl, ToneControl {
	private static final String TAG = SynthPlayer.class.getSimpleName();

	private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "MidletPlayerCallback");
		thread.setUncaughtExceptionHandler((t, e) ->
				Log.e(t.getName(), "UncaughtException in " + t, e));
		return thread;
	});
	private final ArrayList<PlayerListener> listeners = new ArrayList<>();
	private final InternalMetaData metadata;
	private final DataSource dataSource;
	private final long handle;
	private final Library library;

	private Map<String, Control> controls;
	private int state = UNREALIZED;
	private int volume = 100;
	private boolean mute;
	private int pan;

	SynthPlayer(Library library, DataSource dataSource) {
		String locator = dataSource.getLocator();
		if (MIDI_DEVICE_LOCATOR.equals(locator) || TONE_DEVICE_LOCATOR.equals(locator)) {
			metadata = new DeviceMetaData();
		} else {
			metadata = new InternalMetaData();
		}
		this.library = library;
		this.dataSource = dataSource;
		handle = library.createPlayer(locator);
		library.setListener(handle, this);
	}

	@Override
	public void realize() throws MediaException {
		checkClosed();

		if (state == UNREALIZED) {
			library.realize(handle);
			if (controls == null) {
				controls = new HashMap<>();
				if (TONE_DEVICE_LOCATOR.equals(dataSource.getLocator())) {
					controls.put(ToneControl.class.getName(), this);
				}
				controls.put(VolumeControl.class.getName(), this);
				controls.put(MIDIControl.class.getName(), new MIDIControlImpl(this, library, handle));
				controls.put(PanControl.class.getName(), this);
				controls.put(MetaDataControl.class.getName(), metadata);
				controls.put(EqualizerControl.class.getName(), new InternalEqualizer());
			}
			state = REALIZED;
		}
	}

	@Override
	public void prefetch() throws MediaException {
		checkClosed();

		if (state == UNREALIZED) {
			realize();
		}

		if (state == REALIZED) {
			try {
				metadata.updateMetaData(dataSource);
			} catch (Exception e) {
				Log.w(TAG, "prefetch: update metadata failed", e);
			}
			library.prefetch(handle);
			state = PREFETCHED;
		}
	}

	@Override
	public void start() throws MediaException {
		prefetch();

		if (state == PREFETCHED) {
			library.start(handle);

			state = STARTED;
			postEvent(PlayerListener.STARTED, getMediaTime());
		}
	}

	@Override
	public void stop() throws MediaException {
		checkClosed();
		if (state == STARTED) {
			library.pause(handle);

			state = PREFETCHED;
			postEvent(PlayerListener.STOPPED, getMediaTime());
		}
	}

	@Override
	public void deallocate() {
		try {
			stop();
		} catch (MediaException e) {
			Log.e(TAG, "deallocate: stop() failed", e);
		}

		if (state == PREFETCHED) {
			library.deallocate(handle);
			state = REALIZED;
		}
	}

	@Override
	public void close() {
		if (state != CLOSED) {
			state = CLOSED;
			library.close(handle);
		}

		dataSource.disconnect();
		postEvent(PlayerListener.CLOSED, null);
	}

	@Override
	public long setMediaTime(long now) throws MediaException {
		checkRealized();
		return library.setMediaTime(handle, now);
	}

	@Override
	public long getMediaTime() {
		checkClosed();
		if (state < PREFETCHED) {
			return TIME_UNKNOWN;
		} else {
			return library.getMediaTime(handle);
		}
	}

	@Override
	public long getDuration() {
		checkClosed();
		return library.getDuration(handle);
	}

	@Override
	public void setLoopCount(int count) {
		checkClosed();
		if (state == STARTED)
			throw new IllegalStateException("player must not be in STARTED state while using setLoopCount()");

		if (count == 0) {
			throw new IllegalArgumentException("loop count must not be 0");
		}
		library.setRepeat(handle, count);
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public String getContentType() {
		checkRealized();
		return dataSource.getContentType();
	}

	@Override
	public int setPan(int pan) {
		if (pan < -100) {
			pan = -100;
		} else if (pan > 100) {
			pan = 100;
		}
		if (this.pan == pan) {
			return pan;
		}
		this.pan = pan;
		if (state == CLOSED) {
			return pan;
		}
		if (!mute) {
			updateVolume();
		}
		return pan;
	}

	@Override
	public int getPan() {
		return pan;
	}

	@Override
	public void setMute(boolean mute) {
		if (this.mute == mute) {
			return;
		}
		this.mute = mute;
		if (state == CLOSED) {
			return;
		}
		if (mute) {
			library.setVolume(handle, 0, 0);
		} else {
			updateVolume();
		}
		postEvent(PlayerListener.VOLUME_CHANGED, this);
	}

	@Override
	public boolean isMuted() {
		return mute;
	}

	@Override
	public int setLevel(int level) {
		if (level < 0) {
			level = 0;
		} else if (level > 100) {
			level = 100;
		}
		if (volume == level) {
			return level;
		}
		volume = level;
		if (state == CLOSED) {
			return level;
		}
		if (!mute) {
			updateVolume();
		}
		postEvent(PlayerListener.VOLUME_CHANGED, this);
		return level;
	}

	@Override
	public int getLevel() {
		return volume;
	}

	@Override
	public Control getControl(String controlType) {
		checkRealized();
		if (controlType == null) {
			throw new IllegalArgumentException();
		}
		if (!controlType.contains(".")) {
			controlType = "javax.microedition.media.control." + controlType;
		}
		return controls.get(controlType);
	}

	@Override
	public Control[] getControls() {
		checkRealized();
		return controls.values().toArray(new Control[0]);
	}

	@Override
	public synchronized void addPlayerListener(PlayerListener playerListener) {
		checkClosed();
		if (playerListener != null && !listeners.contains(playerListener)) {
			listeners.add(playerListener);
		}
	}

	@Override
	public synchronized void removePlayerListener(PlayerListener playerListener) {
		checkClosed();
		listeners.remove(playerListener);
	}

	/** @noinspection unused */
	@Keep // call from native
	private void postEvent(int type, long time) {
		switch (type) {
			case 1 -> { // restart
				postEvent(PlayerListener.END_OF_MEDIA, time);
				postEvent(PlayerListener.STARTED, 0);
			}
			case 2 -> { // stop
				postEvent(PlayerListener.END_OF_MEDIA, time);
				state = PREFETCHED;
			}
			case 3 -> { // error
				postEvent(PlayerListener.ERROR, null);
				state = PREFETCHED;
			}
		}
	}

	private synchronized void postEvent(String event, Object eventData) {
		for (PlayerListener listener : listeners) {
			// Callbacks should be async
			callbackExecutor.execute(() -> listener.playerUpdate(this, event, eventData));
		}
	}

	private void checkClosed() {
		if (state == CLOSED) {
			throw new IllegalStateException("player is closed");
		}
	}

	private void checkRealized() {
		checkClosed();
		if (state < REALIZED) {
			throw new IllegalStateException("call realize() before using the player");
		}
	}

	@Override
	public void setSequence(byte[] sequence) {
		if (state >= PREFETCHED) {
			throw new IllegalStateException();
		} else if (sequence == null) {
			throw new IllegalArgumentException("sequence is NULL");
		}
		try {
			if (!library.hasToneControl()) {
				ToneSequence tone = new ToneSequence(sequence);
				tone.process();
				sequence = tone.getByteArray();
			}
			library.setDataSource(handle, sequence);
		} catch (Exception e) {
			Log.e(TAG, "setSequence: ", e);
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		library.finalize(handle);
		super.finalize();
	}

	private void updateVolume() {
		float gain = volumeToGain(volume);
		if (pan == 0) {
			library.setVolume(handle, gain, gain);
		} else if (pan < 0) {
			library.setVolume(handle, gain, volumeToGain(volume * (100 + pan) / 100));
		} else {
			library.setVolume(handle, volumeToGain(volume * (100 - pan) / 100), gain);
		}
	}

	private float volumeToGain(int volume) {
		if (volume <= 0) {
			return 0.0f;
		} else if (volume >= 100) {
			return 1.0f;
		}
		return (float) (1 - (Math.log(100 - volume) / Math.log(100)));
	}
}
