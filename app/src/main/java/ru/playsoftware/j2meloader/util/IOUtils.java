/*
 * Copyright 2020 Nikita Shakarun
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

package ru.playsoftware.j2meloader.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import kotlin.io.ByteStreamsKt;
import kotlin.io.ConstantsKt;

public class IOUtils {
	public static byte[] toByteArray(InputStream stream) throws IOException {
		return ByteStreamsKt.readBytes(stream);
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		ByteStreamsKt.copyTo(input, output, ConstantsKt.DEFAULT_BUFFER_SIZE);
	}
}
