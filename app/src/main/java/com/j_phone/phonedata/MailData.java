/*
 * Copyright 2024 Yury Kharchenko
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

package com.j_phone.phonedata;

import java.io.IOException;

public interface MailData extends DataElement {
	int ADREAD = 1;
	int BCC_ADDRESS_INFO = 4;
	int BCC_NAME_INFO = 9;
	int BODY_INFO = 11;
	int CC_ADDRESS_INFO = 3;
	int CC_NAME_INFO = 8;
	int CONFIRM_OFF = 0;
	int CONFIRM_ON = 1;
	int DATE_INFO = 12;
	int FROM_ADDRESS_INFO = 1;
	int FROM_NAME_INFO = 6;
	int MAIL_TYPE_GREETING = 2;
	int MAIL_TYPE_INFO = 13;
	int MAIL_TYPE_SKY = 1;
	int MAIL_TYPE_SUPER = 0;
	int PRIORITY_LOW = 2;
	int PRIORITY_NOMAL = 0;
	int PRIORITY_URGENT = 1;
	int REPLYTO_ADDRESS_INFO = 5;
	int SEND_STATE_CANCEL = 2;
	int SEND_STATE_FAIL = 3;
	int SEND_STATE_MIDST = 0;
	int SEND_STATE_NO_MESSAGE = 4;
	int SEND_STATE_SUCCESS = 1;
	int SUBJECT_INFO = 10;
	int TO_ADDRESS_INFO = 2;
	int TO_NAME_INFO = 7;
	int UNREAD = 0;

	boolean isUnRead() throws IOException;

	boolean hasRemainder() throws IOException;

	int hasSendState() throws IOException;

	int getAttachedFileCount() throws IOException;

	String getAttachedFileName(int index) throws IOException;

	byte[] getAttachedFileData(int index) throws IOException;

	void setState(int state) throws IOException;

	int setAttachedFile(String pathname) throws IOException;

	int setAttachedData(byte[] data, String attachedFileName, int fileType) throws IOException;

	void removeAttachedFile(int index) throws IOException;

	void setConfirm(int confirm) throws IOException;

	void setPriority(int priority) throws IOException;
}
