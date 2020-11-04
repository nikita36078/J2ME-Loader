/*
 * Copyright 2020 Nikita Shakarun
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

package com.samsung.util;

public class SM {
	private String destAddress;
	private String callbackAddress;
	private String data;

	public SM() {
	}

	public SM(String dest, String callback, String textMessage) {
		setDestAddress(dest);
		setCallbackAddress(callback);
		setData(textMessage);
	}

	public String getCallbackAddress() {
		return callbackAddress;
	}

	public String getData() {
		return data;
	}

	public String getDestAddress() {
		return destAddress;
	}

	public void setCallbackAddress(String address) {
		this.callbackAddress = address;
	}

	public void setData(String textMessage) {
		this.data = textMessage;
	}

	public void setDestAddress(String address) {
		this.destAddress = address;
	}
}
