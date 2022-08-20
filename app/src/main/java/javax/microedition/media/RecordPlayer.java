/*
 * Copyright 2019 Nikita Shakarun
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

import android.media.MediaRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.media.control.RecordControl;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.util.IOUtils;

public class RecordPlayer extends BasePlayer implements RecordControl {

	private static final int RECORD_CLOSED = 0;
	private static final int RECORD_PREPARED = 1;
	private static final int RECORD_STARTED = 2;
	private static final int RECORD_STOPPED = 3;

	private MediaRecorder recorder;
	private OutputStream stream;
	private File outputFile;
	private int state;

	public RecordPlayer() {
		recorder = new MediaRecorder();
		state = RECORD_CLOSED;
		addControl(RecordControl.class.getName(), this);
	}

	@Override
	public void setRecordStream(OutputStream stream) {
		if (stream == null) {
			throw new IllegalArgumentException();
		}
		if (state == RECORD_PREPARED || state == RECORD_STARTED){
			throw new IllegalStateException();
		}
		this.stream = stream;
		prepare();
	}

	@Override
	public void setRecordLocation(String locator) throws IOException, MediaException {
		if (locator == null) {
			throw new IllegalArgumentException();
		}
		if (state == RECORD_PREPARED || state == RECORD_STARTED){
			throw new IllegalStateException();
		}
		this.stream = Connector.openOutputStream(locator);
		prepare();
	}

	private void prepare() {
		try {
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
			outputFile = File.createTempFile("record", ".3gpp", ContextHolder.getCacheDir());
			recorder.setOutputFile(outputFile.getAbsolutePath());
			recorder.prepare();
			state = RECORD_PREPARED;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startRecord() {
		if (state != RECORD_PREPARED) {
			throw new IllegalStateException();
		}
		recorder.start();
		state = RECORD_STARTED;
	}

	@Override
	public void stopRecord() {
		if (state == RECORD_STARTED) {
			recorder.stop();
			state = RECORD_STOPPED;
		}
	}

	@Override
	public void commit() throws IOException {
		stopRecord();

		if (state != RECORD_CLOSED) {
			FileInputStream fis = new FileInputStream(outputFile);
			IOUtils.copy(fis, stream);
			fis.close();
			stream.close();
			outputFile.delete();
			state = RECORD_CLOSED;
		}
	}

	@Override
	public int setRecordSizeLimit(int size) throws MediaException {
		return 0;
	}

	@Override
	public void reset() throws IOException {
		stopRecord();

		outputFile.delete();
		prepare();
	}
}
