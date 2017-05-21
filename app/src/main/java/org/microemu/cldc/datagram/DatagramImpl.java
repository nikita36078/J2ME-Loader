/*
 *  MicroEmulator
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.BufferOverflowException;

import javax.microedition.io.Datagram;

/**
 * {@link Datagram} realisation.
 */
public class DatagramImpl implements Datagram {

	/**
	 * The encapsulated {@link DatagramPacket}
	 */
	private DatagramPacket packet;

	/**
	 * Our specialised {@link OutputStream} to write to the packet buffer
	 */
	private BufferOutputStream os;

	/**
	 * Used to write to the packet buffer
	 */
	private DataOutputStream dos;

	/**
	 * Used to read from packet buffer
	 */
	private DataInputStream dis;

	/**
	 * A specialisation of {@link OutputStream} that writes into the
	 * encapsulated {@link DatagramPacket} buffer
	 */
	class BufferOutputStream extends OutputStream {

		private int originalOffset;

		private int offset;

		public BufferOutputStream() {
			this.originalOffset = packet.getOffset();
			this.offset = originalOffset;
		}

		public void write(int b) throws IOException {
			byte[] buffer = packet.getData();
			if (offset > buffer.length - 1) {
				throw new BufferOverflowException();
			}
			buffer[offset++] = (byte) b;
		}

		public void reset() {
			offset = originalOffset;
		}
	}

	/**
	 * Instantiates a new {@link DatagramImpl} with the given buffer size.
	 * 
	 * @param size
	 *            the buffer size
	 * 
	 * @throws IllegalAccessException
	 *             if <tt>size</tt> is negative or equal to zero
	 */
	DatagramImpl(int size) {
		if (size <= 0) {
			throw new IllegalArgumentException("Invalid size: " + size);
		}
		packet = new DatagramPacket(new byte[size], size);
		initialiseInOut();
	}

	/**
	 * Instantiates a new {@link DatagramImpl} with the given buffer.
	 * 
	 * @param buff
	 *            the buffer to use
	 * @param length
	 *            the length of the buffer to use
	 */
	DatagramImpl(byte[] buff, int length) {
		packet = new DatagramPacket(buff, length);
		initialiseInOut();
	}

	/**
	 * Initialises the input and output streams.
	 */
	private void initialiseInOut() {
		os = new BufferOutputStream();
		dos = new DataOutputStream(os);
		dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));
	}

	public String getAddress() {
		return Connection.PROTOCOL + packet.getAddress().getCanonicalHostName() + ":" + packet.getPort();
	}

	public byte[] getData() {
		return packet.getData();
	}

	public int getLength() {
		return packet.getLength();
	}

	public int getOffset() {
		return packet.getOffset();
	}

	public void reset() {
		try {
			os.reset();
			dis.reset();
		} catch (IOException e) {
			// just print it
			e.printStackTrace();
		}
	}

	public void setAddress(String address) throws IOException {
		if (address == null) {
			throw new NullPointerException("address cannot be null");
		}
		if (!address.startsWith(Connection.PROTOCOL)) {
			throw new IllegalArgumentException("Invalid Protocol " + address);
		}
		String noProtocolAddress = address.substring(Connection.PROTOCOL.length());
		int index = noProtocolAddress.indexOf(':');
		if (index == -1) {
			throw new IllegalArgumentException("Missing port in address: " + address);
		}
		String host = noProtocolAddress.substring(0, index);
		String port = noProtocolAddress.substring(index + 1);
		packet.setAddress(InetAddress.getByName(host));
		packet.setPort(Integer.parseInt(port));
	}

	public void setAddress(Datagram reference) {
		packet.setAddress(((DatagramImpl) reference).getDatagramPacket().getAddress());
		packet.setPort(((DatagramImpl) reference).getDatagramPacket().getPort());
	}

	public void setData(byte[] buffer, int offset, int len) {
		packet.setData(buffer, offset, len);
	}

	public void setLength(int len) {
		packet.setLength(len);
	}

	public boolean readBoolean() throws IOException {
		return dis.readBoolean();
	}

	public byte readByte() throws IOException {
		return dis.readByte();
	}

	public char readChar() throws IOException {
		return dis.readChar();
	}

	public double readDouble() throws IOException {
		return dis.readDouble();
	}

	public float readFloat() throws IOException {
		return dis.readFloat();
	}

	public void readFully(byte[] b) throws IOException {
		dis.readFully(b);
	}

	public void readFully(byte[] b, int off, int len) throws IOException {
		dis.read(b, off, len);
	}

	public int readInt() throws IOException {
		return dis.readInt();
	}

	public String readLine() throws IOException {
		return dis.readLine();
	}

	public long readLong() throws IOException {
		return dis.readLong();
	}

	public short readShort() throws IOException {
		return dis.readShort();
	}

	public String readUTF() throws IOException {
		return dis.readUTF();
	}

	public int readUnsignedByte() throws IOException {
		return dis.readUnsignedByte();
	}

	public int readUnsignedShort() throws IOException {
		return dis.readUnsignedShort();
	}

	public int skipBytes(int n) throws IOException {
		return dis.skipBytes(n);
	}

	public void write(int b) throws IOException {
		dos.write(b);
	}

	public void write(byte[] b) throws IOException {
		dos.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		dos.write(b, off, len);
	}

	public void writeBoolean(boolean v) throws IOException {
		dos.writeBoolean(v);
	}

	public void writeByte(int v) throws IOException {
		dos.writeByte(v);
	}

	public void writeBytes(String s) throws IOException {
		dos.writeBytes(s);
	}

	public void writeChar(int v) throws IOException {
		dos.writeChar(v);
	}

	public void writeChars(String v) throws IOException {
		dos.writeChars(v);
	}

	public void writeDouble(double v) throws IOException {
		dos.writeDouble(v);
	}

	public void writeFloat(float v) throws IOException {
		dos.writeFloat(v);
	}

	public void writeInt(int v) throws IOException {
		dos.writeInt(v);
	}

	public void writeLong(long v) throws IOException {
		dos.writeLong(v);
	}

	public void writeShort(int v) throws IOException {
		dos.writeShort(v);
	}

	public void writeUTF(String str) throws IOException {
		dos.writeUTF(str);
	}

	/**
	 * Answers the underlying {@link DatagramPacket}.
	 * 
	 * @return the encapsulated packet
	 */
	DatagramPacket getDatagramPacket() {
		return packet;
	}
}
