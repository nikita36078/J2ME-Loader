/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2020 Nikita Shakarun
 * Copyright 2020-2023 Yury Kharchenko
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

package javax.microedition.media;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.tone.MidiToneConstants;
import javax.microedition.media.tone.ToneSequence;

import kotlin.io.FilesKt;
import ru.woesss.j2me.mmapi.FileCacheDataSource;
import ru.woesss.j2me.mmapi.protocol.device.DeviceMetaData;

public class MicroPlayer extends BasePlayer implements MediaPlayer.OnCompletionListener, ToneControl {
	private static final String TAG = MicroPlayer.class.getSimpleName();

	protected final MediaPlayer player = new AndroidPlayer();
	protected final DataSource source;
	private final InternalMetaData metadata;

	public MicroPlayer(String locator) throws IOException {
		if (!Manager.TONE_DEVICE_LOCATOR.equals(locator)) {
			throw new IllegalArgumentException();
		}
		source = new FileCacheDataSource("audio/x-tone-seq", "mid");
		addControl(MidiToneConstants.TONE_CONTROL_FULL_NAME, this);
		metadata = new DeviceMetaData();
		init();
	}

	MicroPlayer(DataSource datasource) {
		source = datasource;
		metadata = new InternalMetaData();
		init();
	}

	private void init() {
		player.setOnCompletionListener(this);
		addControl(MetaDataControl.class.getName(), metadata);
	}

	@Override
	public synchronized void onCompletion(MediaPlayer mp) {
		complete();
	}

	@Override
	public void doRealize() throws IOException {
		source.connect();
		player.setDataSource(source.getLocator());
	}

	public void doPrefetch() throws IOException {
		metadata.updateMetaData(source);
	}

	public void doStart() {
		player.start();
	}

	public void doStop() {
		player.pause();
	}

	public void doClose() {
		player.release();
		source.disconnect();
	}

	public void doReset() {
		player.reset();
	}

	public void doSetMediaTime(long usec) {
		int time = (int) usec / 1000;
		player.seekTo(time);
	}

	@Override
	public long doGetMediaTime() {
		return player.getCurrentPosition() * 1000L;
	}

	@Override
	public long doGetDuration() {
		return player.getDuration() * 1000L;
	}

	@Override
	public void doSetLooping(boolean looping) {
		player.setLooping(looping);
	}

	@Override
	public String doGetContentType() {
		return source.getContentType();
	}

	@Override
	public void doSetVolume(float left, float right) {
		player.setVolume(left, right);
	}


	// ToneControl

	@Override
	public void setSequence(byte[] sequence) {
		if (state >= PREFETCHED) {
			throw new IllegalStateException();
		} else if (sequence == null) {
			throw new IllegalArgumentException("sequence is NULL");
		}
		try {
			ToneSequence tone = new ToneSequence(sequence);
			tone.process();
			byte[] data = tone.getByteArray();
			String locator = source.getLocator();
			FilesKt.writeBytes(new File(locator), data);
		} catch (Exception e) {
			Log.e(TAG, "setSequence: ", e);
			throw new IllegalArgumentException(e);
		}
	}
}
