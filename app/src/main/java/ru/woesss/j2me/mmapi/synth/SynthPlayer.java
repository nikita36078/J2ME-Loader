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
				String locator = dataSource.getLocator();
				if (MIDI_DEVICE_LOCATOR.equals(locator)) {
					controls.put(MIDIControl.class.getName(), new MIDIControlImpl(this, library, handle));
				} else if (TONE_DEVICE_LOCATOR.equals(locator)) {
					controls.put(ToneControl.class.getName(), this);
					controls.put(VolumeControl.class.getName(), this);
				} else {
					controls.put(VolumeControl.class.getName(), this);
				}
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
			state = UNREALIZED;
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
		return library.setPan(handle, pan);
	}

	@Override
	public int getPan() {
		return library.getPan(handle);
	}

	@Override
	public void setMute(boolean mute) {
		library.setMute(handle, mute);

	}

	@Override
	public boolean isMuted() {
		return library.isMuted(handle);
	}

	@Override
	public int setLevel(int level) {
		return library.setVolume(handle, level);
	}

	@Override
	public int getLevel() {
		return library.getVolume(handle);
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
		if (!listeners.contains(playerListener) && playerListener != null) {
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
			case 1: // restart
				postEvent(PlayerListener.END_OF_MEDIA, time);
				postEvent(PlayerListener.STARTED, 0);
				break;
			case 2: // stop
				postEvent(PlayerListener.END_OF_MEDIA, time);
				state = PREFETCHED;
				break;
			case 3: // error
				postEvent(PlayerListener.ERROR, null);
				state = PREFETCHED;
				break;
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
}
