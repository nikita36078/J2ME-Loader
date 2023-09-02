/*
 * Copyright 2017 Nikita Shakarun
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

package javax.microedition.media.protocol;

import java.io.IOException;

import javax.microedition.media.Controllable;

public interface SourceStream extends Controllable {

	int NOT_SEEKABLE = 0;

	int SEEKABLE_TO_START = 1;

	int RANDOM_ACCESSIBLE = 2;

	ContentDescriptor getContentDescriptor();

	long getContentLength();

	int getSeekType();

	int getTransferSize();

	int read(byte[] buffer, int offset, int length) throws IOException;

	long seek(long pos) throws IOException;

	long tell();
}
