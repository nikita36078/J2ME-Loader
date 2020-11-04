/*
 * Copyright 2020 Nikita Shakarun
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

package javax.microedition.amms;

import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.media.Controllable;
import javax.microedition.media.MediaException;

public interface MediaProcessor extends Controllable {
	public final int UNKNOWN = -1;
	public final int UNREALIZED = 100;
	public final int REALIZED = 200;
	public final int STARTED = 400;
	public final int STOPPED = 300;

	public void setInput(InputStream input, int length) throws javax.microedition.media.MediaException;

	public void setInput(Object image) throws javax.microedition.media.MediaException;

	public void setOutput(OutputStream output);

	public void start() throws MediaException;

	public void stop() throws MediaException;

	public void complete() throws MediaException;

	public void abort();

	public void addMediaProcessorListener(MediaProcessorListener mediaProcessorListener);

	public void removeMediaProcessorListener(MediaProcessorListener mediaProcessorListener);

	public int getProgress();

	public int getState();
}
