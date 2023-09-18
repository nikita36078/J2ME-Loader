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

package ru.woesss.util.zip;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.util.List;

public class ZipFile extends net.lingala.zip4j.ZipFile {

	public ZipFile(File zipFile) {
		super(zipFile);
	}

	@Override
	public FileHeader getFileHeader(String fileName) throws ZipException {
		FileHeader fileHeader = null;
		List<FileHeader> fileHeaders = getFileHeaders();
		for (FileHeader fh : fileHeaders) {
			String name = fh.getFileName();
			if (name == null || name.trim().isEmpty()) {
				continue;
			}
			if (name.equals(fileName)) {
				return fh;
			} else if (fileHeader == null && name.equalsIgnoreCase(fileName)) {
				fileHeader = fh;
			}
		}
		return fileHeader;
	}
}
