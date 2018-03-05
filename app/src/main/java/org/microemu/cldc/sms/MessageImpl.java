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

import java.util.Date;

import javax.wireless.messaging.BinaryMessage;
import javax.wireless.messaging.TextMessage;

public class MessageImpl implements BinaryMessage, TextMessage {

	private byte[] data;
	private String address;

	public MessageImpl(String type, String address) {
		this.address = address;
	}

	@Override
	public byte[] getPayloadData() {
		return data;
	}

	@Override
	public void setPayloadData(byte[] data) {
		this.data = data;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public Date getTimestamp() {
		return new Date();
	}

	@Override
	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String getPayloadText() {
		return new String(data);
	}

	@Override
	public void setPayloadText(String text) {
		this.data = text.getBytes();
	}
}
