/**
 * MicroEmulator
 * Copyright (C) 2001,2002 Bartek Teodorczyk <barteo@barteo.net>
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 */

package org.microemu.cldc.http;

import org.microemu.microedition.io.ConnectionImplementation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.microedition.io.HttpConnection;

public class Connection implements HttpConnection, ConnectionImplementation {

	protected URLConnection cn;
	protected URLConnection cnOut;

	protected boolean connected = false;

	protected static boolean allowNetworkConnection = true;

	@Override
	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		if (!isAllowNetworkConnection()) {
			throw new IOException("No network");
		}
		URL url;
		try {
			url = new URL(name);
		} catch (MalformedURLException ex) {
			throw new IOException(ex.toString());
		}
		cn = url.openConnection();
		// Add encoding info to the header
		cn.setRequestProperty("Accept-Encoding", "identity");
		// J2ME do not follow redirects. Test this url
		// http://www.microemu.org/test/r/
		if (cn instanceof HttpURLConnection) {
			((HttpURLConnection) cn).setInstanceFollowRedirects(false);
		}
		return this;
	}

	@Override
	public void close() throws IOException {
		if (cn == null) {
			return;
		}

		if (cn instanceof HttpURLConnection) {
			((HttpURLConnection) cn).disconnect();
		}

		if (cnOut instanceof HttpURLConnection) {
			((HttpURLConnection) cnOut).disconnect();
		}

		cn = null;
		cnOut = null;
	}

	@Override
	public String getURL() {
		if (cn == null) {
			return null;
		}

		return cn.getURL().toString();
	}

	@Override
	public String getProtocol() {
		return "http";
	}

	@Override
	public String getHost() {
		if (cn == null) {
			return null;
		}

		return cn.getURL().getHost();
	}

	@Override
	public String getFile() {
		if (cn == null) {
			return null;
		}

		return cn.getURL().getFile();
	}

	@Override
	public String getRef() {
		if (cn == null) {
			return null;
		}

		return cn.getURL().getRef();
	}

	@Override
	public String getQuery() {
		if (cn == null) {
			return null;
		}

		// return cn.getURL().getQuery();
		return null;
	}

	@Override
	public int getPort() {
		if (cn == null) {
			return -1;
		}

		int port = cn.getURL().getPort();
		if (port == -1) {
			return 80;
		}
		return port;
	}

	@Override
	public String getRequestMethod() {
		if (cn == null) {
			return null;
		}

		if (cn instanceof HttpURLConnection) {
			return ((HttpURLConnection) cn).getRequestMethod();
		} else {
			return null;
		}
	}

	@Override
	public void setRequestMethod(String method) throws IOException {
		if (cn == null) {
			throw new IOException();
		}

		if (method.equals(HttpConnection.POST)) {
			cn.setDoOutput(true);
		}

		if (cn instanceof HttpURLConnection) {
			((HttpURLConnection) cn).setRequestMethod(method);
		}
	}

	@Override
	public String getRequestProperty(String key) {
		if (cn == null) {
			return null;
		}

		return cn.getRequestProperty(key);
	}

	@Override
	public void setRequestProperty(String key, String value) throws IOException {
		if (cn == null || connected) {
			throw new IOException();
		}

		cn.setRequestProperty(key, value);
	}

	@Override
	public int getResponseCode() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		if (cn instanceof HttpURLConnection) {
			return ((HttpURLConnection) cn).getResponseCode();
		} else {
			return -1;
		}
	}

	@Override
	public String getResponseMessage() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		if (cn instanceof HttpURLConnection) {
			return ((HttpURLConnection) cn).getResponseMessage();
		} else {
			return null;
		}
	}

	@Override
	public long getExpiration() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getExpiration();
	}

	@Override
	public long getDate() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getDate();
	}

	@Override
	public long getLastModified() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getLastModified();
	}

	@Override
	public String getHeaderField(String name) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderField(name);
	}

	@Override
	public int getHeaderFieldInt(String name, int def) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderFieldInt(name, def);
	}

	@Override
	public long getHeaderFieldDate(String name, long def) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderFieldDate(name, def);
	}

	@Override
	public String getHeaderField(int n) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderField(getImplIndex(n));
	}

	@Override
	public String getHeaderFieldKey(int n) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderFieldKey(getImplIndex(n));
	}

	private int getImplIndex(int index) {
		if (cn.getHeaderFieldKey(0) == null && cn.getHeaderField(0) != null) {
			index++;
		}
		return index;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		if (cn == null) {
			throw new IOException();
		}

		connected = true;

		try {
			return cn.getInputStream();
		} catch (IOException ex) {
			if (cn instanceof HttpURLConnection) {
				InputStream errorStream = ((HttpURLConnection) cn).getErrorStream();
				if (errorStream == null) throw ex;
				return errorStream;
			}
			throw ex;
		}
	}

	@Override
	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		if (cn == null) {
			throw new IOException();
		}

		connected = true;

		if (cn instanceof HttpURLConnection &&
				((HttpURLConnection) cn).getRequestMethod().equals(HttpConnection.GET)) {
			if (cnOut == null) {
				cnOut = cn.getURL().openConnection();
				cnOut.setDoOutput(true);
				((HttpURLConnection) cnOut).setRequestMethod(HttpConnection.POST);
			}
			return cnOut.getOutputStream();
		} else {
			return cn.getOutputStream();
		}
	}

	@Override
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	@Override
	public String getType() {
		try {
			return getHeaderField("content-type");
		} catch (IOException ex) {
			return null;
		}
	}

	@Override
	public String getEncoding() {
		try {
			return getHeaderField("content-encoding");
		} catch (IOException ex) {
			return null;
		}
	}

	@Override
	public long getLength() {
		try {
			return getHeaderFieldInt("content-length", -1);
		} catch (IOException ex) {
			return -1;
		}
	}

	public static boolean isAllowNetworkConnection() {
		return allowNetworkConnection;
	}

	public static void setAllowNetworkConnection(boolean allowNetworkConnection) {
		Connection.allowNetworkConnection = allowNetworkConnection;
	}

}
