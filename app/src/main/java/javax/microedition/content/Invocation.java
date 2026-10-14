/*
 * Copyright 2026 Nikita Shakarun
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

package javax.microedition.content;

import java.io.IOException;

import javax.microedition.io.Connection;

public final class Invocation {
	public static final int INIT = 1;
	public static final int ACTIVE = 2;
	public static final int WAITING = 3;
	public static final int HOLD = 4;
	public static final int OK = 5;
	public static final int CANCELLED = 6;
	public static final int ERROR = 7;
	public static final int INITIATED = 8;

	private String url;
	private String type;
	private String id;
	private String action;
	private String[] args;
	private byte[] data;
	private boolean responseRequired = true;
	private int status = INIT;

	public Invocation() {
	}

	public Invocation(String url) {
		this(url, null, null);
	}

	public Invocation(String url, String type) {
		this(url, type, null);
	}

	public Invocation(String url, String type, String ID) {
		this(url, type, ID, true, null);
	}

	public Invocation(String url, String type, String ID, boolean responseRequired, String action) {
		this.url = url;
		this.type = type;
		this.id = ID;
		this.responseRequired = responseRequired;
		this.action = action;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public String[] getArgs() {
		return args;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public boolean getResponseRequired() {
		return responseRequired;
	}

	public void setResponseRequired(boolean responseRequired) {
		this.responseRequired = responseRequired;
	}

	public int getStatus() {
		return status;
	}

	public String getID() {
		return id;
	}

	public void setID(String ID) {
		this.id = ID;
	}

	public Invocation getPrevious() {
		return null;
	}

	public String getInvokingAuthority() {
		return null;
	}

	public String getInvokingID() {
		return null;
	}

	public String getInvokingAppName() {
		return null;
	}

	public String findType() throws IOException, ContentHandlerException, SecurityException {
		return type;
	}

	public Connection open(boolean timeouts) throws IOException, SecurityException {
		return null;
	}

	public void setCredentials(String username, char[] password) {
	}
}
