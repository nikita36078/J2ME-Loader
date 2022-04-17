/*
 *  Copyright 2022 Yury Kharchenko
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

package javax.wireless.messaging;

interface MultipartMessage extends Message {

	void addMessagePart(MessagePart part) throws SizeExceededException;

	boolean removeMessagePartId(String contentID);

	boolean removeMessagePart(MessagePart part);

	boolean removeMessagePartLocation(String contentLocation);

	MessagePart getMessagePart(String contentID);

	String getStartContentId();

	MessagePart[] getMessageParts();

	void setStartContentId(String contentId);

	String getSubject();

	String getHeader(String headerField);

	void setSubject(String subject);

	void setHeader(String headerField, String headerValue);

	boolean addAddress(String type, String address);

	void removeAddresses();

	void removeAddresses(String type);

	boolean removeAddress(String type, String address);

	String[] getAddresses(String type);

	String getAddress();

	void setAddress(String addr);
}
