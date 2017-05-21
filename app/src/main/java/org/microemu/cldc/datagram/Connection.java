/*
 *  MicroEmulator
 *  Copyright (C) 2006 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2007 Ludovic Dewailly <ludovic.dewailly@dreameffect.org>
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

package org.microemu.cldc.datagram;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.UDPDatagramConnection;

import org.microemu.microedition.io.ConnectionImplementation;

/**
 * {@link ConnectionImplementation} for the datagram protocol (UDP).
 */
public class Connection implements DatagramConnection, UDPDatagramConnection, ConnectionImplementation {

	/**
	 * The datagram protocol constant
	 */
	public final static String PROTOCOL = "datagram://";

	/**
	 * The encapsulated {@link DatagramSocket}
	 */
	private DatagramSocket socket;

	/**
	 * The connection address in the format <tt>host:port</tt>
	 */
	private String address;

	public void close() throws IOException {
		socket.close();
	}

	public int getMaximumLength() throws IOException {
		return Math.min(socket.getReceiveBufferSize(), socket.getSendBufferSize());
	}

	public int getNominalLength() throws IOException {
		return getMaximumLength();
	}

	public void send(Datagram dgram) throws IOException {
		socket.send(((DatagramImpl) dgram).getDatagramPacket());
	}

	public void receive(Datagram dgram) throws IOException {
		socket.receive(((DatagramImpl) dgram).getDatagramPacket());
	}

	public Datagram newDatagram(int size) throws IOException {
		return newDatagram(size, address);
	}

	public Datagram newDatagram(int size, String addr) throws IOException {
		if (!addr.startsWith(PROTOCOL)) {
			throw new IllegalArgumentException("Invalid Protocol " + addr);
		}
		Datagram datagram = new DatagramImpl(size);
		datagram.setAddress(addr);
		return datagram;
	}

	public Datagram newDatagram(byte[] buf, int size) throws IOException {
		return newDatagram(buf, size, address);
	}

	public Datagram newDatagram(byte[] buf, int size, String addr) throws IOException {
		if (!addr.startsWith(PROTOCOL)) {
			throw new IllegalArgumentException("Invalid Protocol " + addr);
		}
		Datagram datagram = new DatagramImpl(buf, size);
		datagram.setAddress(addr);
		return datagram;
	}

	public String getLocalAddress() throws IOException {
		InetAddress address = socket.getInetAddress();
		if (address == null) {
			/*
			 * server mode we get the localhost from InetAddress otherwise we
			 * get '0.0.0.0'
			 */
			address = InetAddress.getLocalHost();
		} else {
			/*
			 * client mode we can get the localhost from the socket here
			 */
			address = socket.getLocalAddress();
		}
		return address.getHostAddress();
	}

	public int getLocalPort() throws IOException {
		return socket.getLocalPort();
	}

	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		if (!org.microemu.cldc.http.Connection.isAllowNetworkConnection()) {
			throw new IOException("No network");
		}
		if (!name.startsWith(PROTOCOL)) {
			throw new IOException("Invalid Protocol " + name);
		}
		// TODO currently we ignore the mode
		address = name.substring(PROTOCOL.length());
		int port = -1;
		int index = address.indexOf(':');
		if (index == -1) {
			throw new IllegalArgumentException("Port missing");
		}
		String portToParse = address.substring(index + 1);
		if (portToParse.length() > 0) {
			port = Integer.parseInt(portToParse);
		}
		if (index == 0) {
			// server mode
			if (port == -1) {
				socket = new DatagramSocket();
			} else {
				socket = new DatagramSocket(port);
			}
		} else {
			// client mode
			if (port == -1) {
				throw new IllegalArgumentException("Port missing");
			}
			String host = address.substring(0, index);
			socket = new DatagramSocket();
			socket.connect(InetAddress.getByName(host), port);
		}
		return this;
	}
}
