/*
 *  Copyright 2020 Yury Kharchenko
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

package ru.woesss.j2me.jar;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.util.Log;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import ru.playsoftware.j2meloader.R;

public class Descriptor {
	private static final String TAG = Descriptor.class.getName();

	private static final char UNICODE_BOM = '\uFEFF';
	private static final String MANIFEST_VERSION = "Manifest-Version";
	// required in JAD and Manifest
	public static final String MIDLET_NAME = "MIDlet-Name";
	public static final String MIDLET_VERSION = "MIDlet-Version";
	public static final String MIDLET_VENDOR = "MIDlet-Vendor";

	// required in JAD
	public static final String MIDLET_JAR_URL = "MIDlet-Jar-URL";
	private static final String MIDLET_JAR_SIZE = "MIDlet-Jar-Size";

	// required in JAD and/or Manifest
	private static final String MIDLET_N = "MIDlet-";
	private static final String MICROEDITION_PROFILE = "MicroEdition-Profile";
	private static final String MICROEDITION_CONFIGURATION = "MicroEdition-Configuration";

	// optional
	public static final String MIDLET_CERTIFICATE_N_S = "MIDlet-Certificate-";
	public static final String MIDLET_DATA_SIZE = "MIDlet-Data-Size";
	public static final String MIDLET_DELETE_CONFIRM = "MIDlet-Delete-Confirm ";
	public static final String MIDLET_DELETE_NOTIFY = "MIDlet-Delete-Notify";
	private static final String MIDLET_DESCRIPTION = "MIDlet-Description";
	private static final String MIDLET_ICON = "MIDlet-Icon";
	public static final String MIDLET_INFO_URL = "MIDlet-Info-URL";
	public static final String MIDLET_INSTALL_NOTIFY = "MIDlet-Install-Notify";
	public static final String MIDLET_JAR_RSA_SHA1 = "MIDlet-Jar-RSA-SHA1";
	public static final String MIDLET_PERMISSIONS = "MIDlet-Permissions";
	public static final String MIDLET_PERMISSIONS_OPT = "MIDlet-Permissions-Opt";
	public static final String MIDLET_PUSH_N = "MIDlet-Push-";

	private static final String FAIL_ATTRIBUTE = "Fail attribute '%s: %s'";
	private final boolean isJad;
	private final Map<String, String> attributes = new HashMap<>();

	public Descriptor(String source, boolean isJad) throws IOException {
		this.isJad = isJad;
		init(source);
		if (isJad) {
			verifyJadAttrs();
		}
		verify();

	}

	public Descriptor(File file, boolean isJad) throws IOException {
		this.isJad = isJad;
		byte[] buf;
		try (InputStream inputStream = new FileInputStream(file)) {
			int count = inputStream.available();
			buf = new byte[count];
			int n = 0;
			while (n < buf.length) {
				int read = inputStream.read(buf, n, buf.length - n);
				if (read < 0) {
					throw new EOFException();
				}
				n += read;
			}
		}
		init(new String(buf));
		if (isJad) {
			verifyJadAttrs();
		}
		verify();

	}

	public int compareVersion(String version) {
		if (version == null) return 1;
		String[] mv = getVersion().split("\\.");
		String[] ov = version.split("\\.");
		int len = Math.max(mv.length, ov.length);
		for (int i = 0; i < len; i++) {
			int m = 0;
			if (i < mv.length) {
				try {
					m = Integer.parseInt(mv[i].trim());
				} catch (NumberFormatException ignored) { }
			}
			int o = 0;
			if (i < ov.length) {
				try {
					o = Integer.parseInt(ov[i].trim());
				} catch (NumberFormatException ignored) { }
			}
			if (m != o) {
				return Integer.signum(m - o);
			}
		}

		return 0;
	}

	private void verifyJadAttrs() throws DescriptorException {
		String jarSize = getJarSize();
		if (jarSize == null)
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_JAR_SIZE, "not found"));
		String trim = jarSize.trim();
		if (trim.isEmpty())
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_JAR_SIZE, "empty value"));
		try {
			Integer.parseInt(trim);
		} catch (NumberFormatException e) {
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_JAR_SIZE, jarSize), e);
		}
		attributes.put(MIDLET_JAR_SIZE, trim);
		String url = attributes.get(MIDLET_JAR_URL);
		if (url == null) {
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_JAR_URL, "not found"));
		} else if (url.trim().isEmpty()) {
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_JAR_URL, "empty value"));
		}
		attributes.put(MIDLET_JAR_URL, url.trim());
	}

	private void verify() throws DescriptorException {
		if (getName() == null)
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_NAME, "not found"));
		if (getVendor() == null)
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_VENDOR, "not found"));
		if (getVersion() == null)
			throw new DescriptorException(String.format(FAIL_ATTRIBUTE, MIDLET_VERSION, "not found"));
	}

	public String getVersion() {
		return attributes.get(MIDLET_VERSION);
	}

	public final Map<String, String> getAttrs() {
		return attributes;
	}

	private void init(String source) throws DescriptorException {
		try {
			parse(source);
		} catch (Exception e) {
			Log.e(TAG, source);
			throw new DescriptorException("Bad descriptor", e);
		}
	}

	private void parse(String source) {
		String[] lines = source.split("[\\n\\r]+");
		int length = lines.length;
		if (length == 0) {
			throw new IllegalArgumentException("Descriptor source is empty");
		}
		String line0 = lines[0];
		if (line0.charAt(0) == UNICODE_BOM)
			lines[0] = line0.substring(1);
		Map<String, String> attrs = attributes;
		final StringBuilder sb = new StringBuilder("1.0");
		String key = MANIFEST_VERSION;
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				continue;
			}
			int colon = line.indexOf(':');
			if (colon == -1) {
				if (line.charAt(0) == ' ') sb.append(line, 1, line.length());
				else sb.append(line);
			} else {
				attrs.put(key, sb.toString());
				sb.setLength(0);
				key = line.substring(0, colon++).trim();
				if (line.charAt(colon) == ' ')
					colon++;
				sb.append(line, colon, line.length());
			}
		}
		attrs.put(key, sb.toString());
	}

	private String getFileLocation(String jarURL) {
		Uri jarUri = Uri.parse(jarURL);
		if ("http".equalsIgnoreCase(jarUri.getScheme()) || "https".equalsIgnoreCase(jarUri.getScheme())) {
			return "Интернет";
		}
		return "Файл";
	}

	public String getName() {
		return attributes.get(MIDLET_NAME);
	}

	private static String getSizePretty(String number) {
		long size = Long.parseLong(number.trim());
		DecimalFormat decimalformat = new DecimalFormat("########.00");
		String formatted;
		if (size >= 1024L) {
			float kb = (float) size / 1024F;
			if (kb >= 1024F) {
				float mb = kb / 1024F;
				if (mb >= 1024F) {
					float gb = mb / 1024F;
					formatted = decimalformat.format(gb) + " GB";
				} else {
					formatted = decimalformat.format(mb) + " MB";
				}
			} else {
				formatted = decimalformat.format(kb) + " KB";
			}
		} else {
			formatted = size + " B";
		}
		return formatted;
	}

	public String getVendor() {
		return attributes.get(MIDLET_VENDOR);
	}

	public String getIcon() throws DescriptorException {
		String icon = attributes.get(MIDLET_ICON);
		if (icon == null || icon.trim().isEmpty()) {
			String midlet = MIDLET_N + 1;
			icon = attributes.get(midlet);
			if (icon == null) {
				throw new DescriptorException(String.format(FAIL_ATTRIBUTE, midlet, "not found"));
			}
			int start = icon.indexOf(',');
			if (start != -1) {
				int end = icon.indexOf(',', ++start);
				if (end != -1)
					icon = icon.substring(start, end);
			}
		}
		icon = icon.trim();
		if (icon.isEmpty()) return null;
		while (icon.charAt(0) == '/') {
			icon = icon.substring(1);
		}
		return icon;
	}

	public String getJarUrl() {
		return attributes.get(MIDLET_JAR_URL);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Descriptor))
			return false;
		Descriptor o = (Descriptor) obj;
		return getName().equals(o.getName()) && getVendor().equals(o.getVendor());
	}

	public SpannableStringBuilder getInfo(Context c) {
		SpannableStringBuilder info = new SpannableStringBuilder();
		String description = attributes.get(MIDLET_DESCRIPTION);
		if (description != null) {
			info.append(description).append("\n\n");
		}
		info.append(c.getText(R.string.midlet_name)).append(getName()).append('\n');
		info.append(c.getText(R.string.midlet_vendor)).append(getVendor()).append('\n');
		info.append(c.getText(R.string.midlet_version)).append(getVersion()).append('\n');
		String jarSize = getJarSize();
		if (jarSize != null) {
			info.append(c.getText(R.string.midlet_size)).append(getSizePretty(jarSize)).append('\n');
		}
		info.append('\n');
		return info;
	}

	private String getJarSize() {
		return isJad ? attributes.get(MIDLET_JAR_SIZE) : null;
	}

	public void merge(Descriptor newDescriptor) {
		attributes.putAll(newDescriptor.attributes);
	}

	public void writeTo(File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			writeTo(fos);
		}
	}

	private void writeTo(OutputStream outputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}
		outputStream.write(sb.toString().getBytes());
	}
}
