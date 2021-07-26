/*
 * Copyright 2015 fkrauthan
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

package ru.playsoftware.j2meloader.crashes;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TLSSocketFactory extends SSLSocketFactory {

	private final SSLSocketFactory internalSSLSocketFactory;

	public TLSSocketFactory(Context context) {
		SSLSocketFactory factory;
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (InputStream in = context.getAssets().open("appcenter.cer")) {
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509","BC");
				X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(in);

				trustStore.load(null);
				trustStore.setCertificateEntry("alias", cert);
			}

			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(trustStore);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
			factory = sslContext.getSocketFactory();
		} catch (GeneralSecurityException | IOException e) {
			factory = HttpsURLConnection.getDefaultSSLSocketFactory();
		}
		internalSSLSocketFactory = factory;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return internalSSLSocketFactory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return internalSSLSocketFactory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket() throws IOException {
		return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
		return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
	}

	private Socket enableTLSOnSocket(Socket socket) {
		if ((socket instanceof SSLSocket)) {
			((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.2"});
		}
		return socket;
	}
}
