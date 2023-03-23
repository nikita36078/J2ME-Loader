/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
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

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;

import java.io.IOException;

import javax.microedition.amms.control.PanControl;
import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.protocol.DataSource;

public class MicroPlayer extends BasePlayer implements MediaPlayer.OnCompletionListener,
		VolumeControl, PanControl {
	protected DataSource source;
	private final MediaPlayer player;
	private final InternalMetaData metadata;

	public MicroPlayer(DataSource datasource) {
		player = new AndroidPlayer();
		player.setOnCompletionListener(this);

		source = datasource;
		metadata = new InternalMetaData();

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
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(source.getLocator());
		metadata.updateMetaData(retriever);
		retriever.release();
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

}
