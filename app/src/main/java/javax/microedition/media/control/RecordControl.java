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

package javax.microedition.media.control;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

public interface RecordControl extends Control {
	public void setRecordStream(OutputStream stream);

	public void setRecordLocation(String locator) throws IOException, MediaException;

	public String getContentType();

	public void startRecord();

	public void stopRecord();

	public void commit() throws IOException;

	public int setRecordSizeLimit(int size) throws MediaException;

	public void reset() throws IOException;
}
