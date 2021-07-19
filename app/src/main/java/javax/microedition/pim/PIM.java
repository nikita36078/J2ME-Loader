/*
 *  Copyright 2021 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.microedition.pim;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public abstract class PIM {
	public static final int CONTACT_LIST = 1;
	public static final int EVENT_LIST = 2;
	public static final int TODO_LIST = 3;
	public static final int READ_ONLY = 1;
	public static final int WRITE_ONLY = 2;
	public static final int READ_WRITE = 3;

	protected PIM() {
	}

	public static PIM getInstance() {
		throw new SecurityException("Can't have access to contacts");
	}

	public abstract PIMList openPIMList(int pimListType, int mode) throws PIMException;

	public abstract PIMList openPIMList(int pimListType, int mode, String name) throws PIMException;

	public abstract String[] listPIMLists(int pimListType);

	public abstract PIMItem[] fromSerialFormat(InputStream is, String enc)
			throws PIMException, UnsupportedEncodingException;

	public abstract void toSerialFormat(PIMItem item, OutputStream os, String enc, String dataFormat)
			throws PIMException, UnsupportedEncodingException;

	public abstract String[] supportedSerialFormats(int pimListType);
}
