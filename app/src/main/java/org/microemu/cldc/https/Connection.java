/*
 *  MicroEmulator
 *  Copyright (C) 2006 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package org.microemu.cldc.https;

import org.microemu.cldc.CertificateImpl;
import org.microemu.cldc.SecurityInfoImpl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.microedition.io.HttpsConnection;
import javax.microedition.io.SecurityInfo;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class Connection extends org.microemu.cldc.http.Connection implements HttpsConnection {

	private SSLContext sslContext;

	private SecurityInfo securityInfo;

	public Connection() {
		try {
			sslContext = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}

		securityInfo = null;
	}

	@Override
	public SecurityInfo getSecurityInfo() throws IOException {
		if (securityInfo == null) {
			if (cn == null) {
				throw new IOException();
			}
			if (!connected) {
				cn.connect();
				connected = true;
			}
			HttpsURLConnection https = (HttpsURLConnection) cn;

			Certificate[] certs = https.getServerCertificates();
			if (certs.length == 0) {
				throw new IOException();
			}
			securityInfo = new SecurityInfoImpl(
					https.getCipherSuite(),
					sslContext.getProtocol(),
					new CertificateImpl((X509Certificate) certs[0]));
		}

		return securityInfo;
	}

	@Override
	public String getProtocol() {
		return "https";
	}


	/**
	 * Returns the network port number of the URL for this HttpsConnection
	 *
	 * @return the network port number of the URL for this HttpsConnection. The default HTTPS port number (443) is returned if there was no port number in the string passed to Connector.open.
	 */
	@Override
	public int getPort() {
		if (cn == null) {
			return -1;
		}
		int port = cn.getURL().getPort();
		if (port == -1) {
			return 443;
		}
		return port;
	}

}
