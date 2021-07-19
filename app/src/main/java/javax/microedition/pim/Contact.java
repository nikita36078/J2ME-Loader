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

public interface Contact extends PIMItem {
	int ADDR = 100;
	int BIRTHDAY = 101;
	int CLASS = 102;
	int EMAIL = 103;
	int FORMATTED_ADDR = 104;
	int FORMATTED_NAME = 105;
	int NAME = 106;
	int NICKNAME = 107;
	int NOTE = 108;
	int ORG = 109;
	int PHOTO = 110;
	int PHOTO_URL = 111;
	int PUBLIC_KEY = 112;
	int PUBLIC_KEY_STRING = 113;
	int REVISION = 114;
	int TEL = 115;
	int TITLE = 116;
	int UID = 117;
	int URL = 118;
	int ATTR_ASST = 1;
	int ATTR_AUTO = 2;
	int ATTR_FAX = 4;
	int ATTR_HOME = 8;
	int ATTR_MOBILE = 16;
	int ATTR_OTHER = 32;
	int ATTR_PAGER = 64;
	int ATTR_PREFERRED = 128;
	int ATTR_SMS = 256;
	int ATTR_WORK = 512;
	int ADDR_POBOX = 0;
	int ADDR_EXTRA = 1;
	int ADDR_STREET = 2;
	int ADDR_LOCALITY = 3;
	int ADDR_REGION = 4;
	int ADDR_POSTALCODE = 5;
	int ADDR_COUNTRY = 6;
	int NAME_FAMILY = 0;
	int NAME_GIVEN = 1;
	int NAME_OTHER = 2;
	int NAME_PREFIX = 3;
	int NAME_SUFFIX = 4;
	int CLASS_CONFIDENTIAL = 200;
	int CLASS_PRIVATE = 201;
	int CLASS_PUBLIC = 202;

	int getPreferredIndex(int field);
}
