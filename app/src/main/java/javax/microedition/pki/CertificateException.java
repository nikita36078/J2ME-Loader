/*
 *  MicroEmulator
 *  Copyright (C) 2001-2006 Bartek Teodorczyk <barteo@barteo.net>
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

package javax.microedition.pki;

import java.io.IOException;

public class CertificateException extends IOException {

    private static final long serialVersionUID = 1L;

    public static final byte BAD_EXTENSIONS = 1;
	public static final byte CERTIFICATE_CHAIN_TOO_LONG = 2;
	public static final byte EXPIRED = 3;
	public static final byte UNAUTHORIZED_INTERMEDIATE_CA = 4;
	public static final byte MISSING_SIGNATURE = 5;
	public static final byte NOT_YET_VALID = 6;
	public static final byte SITENAME_MISMATCH = 7;
	public static final byte UNRECOGNIZED_ISSUER = 8;
	public static final byte UNSUPPORTED_SIGALG = 9;
	public static final byte INAPPROPRIATE_KEY_USAGE = 10;
	public static final byte BROKEN_CHAIN = 11;
	public static final byte ROOT_CA_EXPIRED = 12;
	public static final byte UNSUPPORTED_PUBLIC_KEY_TYPE = 13;
	public static final byte VERIFICATION_FAILED = 14;
	
	private Certificate certificate;
	private byte status;
	
	public CertificateException(Certificate certificate, byte status) {
		super();
		
		this.certificate = certificate;
		this.status = status;
	}
	
	public CertificateException(String message, Certificate certificate, byte status) {
		super(message);
		
		this.certificate = certificate;
		this.status = status;
	}
	
	public Certificate getCertificate() {
		return certificate;
	}
	
	public byte getReason() {
		return status;
	}
	
}
