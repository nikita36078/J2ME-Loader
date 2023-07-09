/*
 * Copyright 2018 Nikita Shakarun
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

package org.microemu.cldc.sms;

import org.microemu.microedition.io.ConnectionImplementation;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.wireless.messaging.Message;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.MessageListener;
import javax.wireless.messaging.TextMessage;

public class Connection implements MessageConnection, ConnectionImplementation {

	private static final int MAX_PORT = 65535;

	private MessageListener listener;
	private String name;
	private String address;
	private boolean noMessages;
	private boolean closed;

	@Override
	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		String host, port;
		String address = name.substring("sms://".length());
		int portSepIndex = address.lastIndexOf(':');
		if (portSepIndex >= 0) {
			port = address.substring(portSepIndex + 1);
			host = address.substring(0, portSepIndex);
		} else {
			port = "";
			host = address;
		}
		if (host.length() > 0) {
			validateHost(host);
		}
		if (port.length() > 0) {
			validatePort(port);
		}
		this.name = name;
		this.address = address;
		return this;
	}

	@Override
	public Message newMessage(String type) {
		return newMessage(type, address);
	}

	@Override
	public Message newMessage(String type, String address) {
		Message message;
		if (type.equals(TEXT_MESSAGE)) {
			message = new TextMessageImpl(address, 0);
		} else if (type.equals(BINARY_MESSAGE)) {
			message = new BinaryMessageImpl(address, 0);
		} else {
			throw new IllegalArgumentException("Message type is invalid: " + type);
		}
		return message;
	}

	@Override
	public int numberOfSegments(Message message) {
		return 1;
	}

	@Override
	public Message receive() throws IOException, InterruptedIOException {
		while (noMessages && !closed) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		TextMessage message = new TextMessageImpl(address, System.currentTimeMillis());
		message.setPayloadText("sms");
		noMessages = true;
		return message;
	}

	@Override
	public void send(Message message) throws IOException, InterruptedIOException {
		if (message == null) {
			throw new NullPointerException();
		}
		if (listener != null) {
			Connection connection = (Connection) openConnection(name, 0, false);
			listener.notifyIncomingMessage(connection);
		}
	}

	@Override
	public void setMessageListener(MessageListener listener) throws IOException {
		this.listener = listener;
	}

	@Override
	public void close() throws IOException {
		closed = true;
	}

	private void validateHost(String host) {
		char ch;
		for (int i = 0; i < host.length(); i++) {
			ch = host.charAt(i);
			if (i == 0 && ch == '+') {
				continue;
			}
			if (!Character.isDigit(ch)) {
				throw new IllegalArgumentException("Invalid SMS number");
			}
		}
	}

	private void validatePort(String port) {
		for (int i = 0; i < port.length(); i++) {
			if (!Character.isDigit(port.charAt(i))) {
				throw new IllegalArgumentException("Invalid SMS port");
			}
		}
		int portValue = Integer.parseInt(port);
		if (portValue > MAX_PORT || portValue < 0) {
			throw new IllegalArgumentException("Invalid SMS port");
		}
	}
}
