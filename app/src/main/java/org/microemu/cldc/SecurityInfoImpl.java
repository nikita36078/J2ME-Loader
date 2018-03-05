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

package org.microemu.cldc;

import javax.microedition.io.SecurityInfo;
import javax.microedition.pki.Certificate;

public class SecurityInfoImpl implements SecurityInfo {

	private String cipherSuite;
	private String protocolName;
	private Certificate certificate;

	public SecurityInfoImpl(String cipherSuite, String protocolName, Certificate certificate) {
		this.cipherSuite = cipherSuite;
		this.protocolName = protocolName;
		this.certificate = certificate;
	}

	@Override
	public String getCipherSuite() {
		return cipherSuite;
	}

	@Override
	public String getProtocolName() {
		if (protocolName.startsWith("TLS")) {
			return "TLS";
		} else if (protocolName.startsWith("SSL")) {
			return "SSL";
		} else {
			// TODO Auto-generated method stub
			try {
				throw new RuntimeException();
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	@Override
	public String getProtocolVersion() {
		if (protocolName.startsWith("TLS")) {
			return "3.1";
		} else if (getProtocolName().equals("SSL")) {
			return "3.0";
		} else {
			// TODO Auto-generated method stub
			try {
				throw new RuntimeException();
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	@Override
	public Certificate getServerCertificate() {
		return certificate;
	}

}
