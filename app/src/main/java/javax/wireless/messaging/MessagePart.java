/*
 *  Copyright 2022 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.wireless.messaging;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.playsoftware.j2meloader.util.IOUtils;

public class MessagePart {
	private final String mimeType;
	private final String contentId;
	private final String contentLocation;
	private final String enc;
	private final byte[] contents;

	public MessagePart(InputStream is,
					   String mimeType,
					   String contentId,
					   String contentLocation,
					   String enc)
			throws IOException, SizeExceededException {
		this.contents = IOUtils.toByteArray(is);
		this.mimeType = mimeType;
		this.contentId = contentId;
		this.contentLocation = contentLocation;
		this.enc = enc;
	}

	public MessagePart(byte[] contents,
					   String mimeType,
					   String contentId,
					   String contentLocation,
					   String enc)
			throws SizeExceededException {
		if (mimeType == null || contentId == null)
			throw new IllegalArgumentException();
		this.contents = contents.clone();
		this.mimeType = mimeType;
		this.contentId = contentId;
		this.contentLocation = contentLocation;
		this.enc = enc;
	}

	public MessagePart(byte[] contents,
					   int offset,
					   int length,
					   String mimeType,
					   String contentId,
					   String contentLocation,
					   String enc)
			throws SizeExceededException {
		if (mimeType == null ||
				contentId == null ||
				length < 0 ||
				offset < 0 ||
				offset + length > contents.length)
			throw new IllegalArgumentException();
		byte[] copy = new byte[length];
		System.arraycopy(contents, offset, copy, 0, length);
		this.contents = copy;
		this.mimeType = mimeType;
		this.contentId = contentId;
		this.contentLocation = contentLocation;
		this.enc = enc;
	}

	public byte[] getContent() {
		return contents;
	}

	public java.io.InputStream getContentAsStream() {
		return new ByteArrayInputStream(contents);
	}

	public String getContentID() {
		return contentId;
	}

	public String getMIMEType() {
		return mimeType;
	}

	public String getEncoding() {
		return enc;
	}

	public String getContentLocation() {
		return contentLocation;
	}

	public int getLength() {
		return contents.length;
	}

	public boolean equals(Object o) {
		return super.equals(o);
	}

	public int hashCode() {
		return super.hashCode();
	}

	@NonNull
	public String toString() {
		return super.toString();
	}
}
