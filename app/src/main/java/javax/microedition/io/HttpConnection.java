/*
 *  MicroEmulator
 *  Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
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
 
package javax.microedition.io;

import java.io.IOException;


public interface HttpConnection extends ContentConnection
{

	static final String HEAD = "HEAD";
	static final String GET = "GET";
	static final String POST = "POST";
	
	static final int HTTP_OK = 200;
	static final int HTTP_CREATED = 201;
	static final int HTTP_ACCEPTED = 202;
	static final int HTTP_NOT_AUTHORITATIVE = 203;
	static final int HTTP_NO_CONTENT = 204;
	static final int HTTP_RESET = 205;
	static final int HTTP_PARTIAL = 206;
	
	static final int HTTP_MULT_CHOICE = 300;
	static final int HTTP_MOVED_PERM = 301;
	static final int HTTP_MOVED_TEMP = 302;
	static final int HTTP_SEE_OTHER = 303;
	static final int HTTP_NOT_MODIFIED = 304;
	static final int HTTP_USE_PROXY = 305;
	static final int HTTP_TEMP_REDIRECT = 307;

	static final int HTTP_BAD_REQUEST = 400;
	static final int HTTP_UNAUTHORIZED = 401;
	static final int HTTP_PAYMENT_REQUIRED = 402;
	static final int HTTP_FORBIDDEN = 403;
	static final int HTTP_NOT_FOUND = 404;
	static final int HTTP_BAD_METHOD = 405;
	static final int HTTP_NOT_ACCEPTABLE = 406;
	static final int HTTP_PROXY_AUTH = 407;
	static final int HTTP_CLIENT_TIMEOUT = 408;
	static final int HTTP_CONFLICT = 409;
	static final int HTTP_GONE = 410;
	static final int HTTP_LENGTH_REQUIRED = 411;
	static final int HTTP_PRECON_FAILED = 412;
	static final int HTTP_ENTITY_TOO_LARGE = 413;
	static final int HTTP_REQ_TOO_LONG = 414;
	static final int HTTP_UNSUPPORTED_TYPE = 415;
	static final int HTTP_UNSUPPORTED_RANGE = 416;
	static final int HTTP_EXPECT_FAILED = 417;
	
	static final int HTTP_INTERNAL_ERROR = 500;
	static final int HTTP_NOT_IMPLEMENTED = 501;
	static final int HTTP_BAD_GATEWAY = 502;
	static final int HTTP_UNAVAILABLE = 503;
	static final int HTTP_GATEWAY_TIMEOUT = 504;
	static final int HTTP_VERSION = 505;
	
	
	String getURL();
	
	String getProtocol();
	
	String getHost();
	
	String getFile();
	
	String getRef();
	
	String getQuery();
	
	int getPort();
	
	String getRequestMethod();
	
	void setRequestMethod(String method)
      throws IOException;
      
	String getRequestProperty(String key);
	
	void setRequestProperty(String key, String value)
      throws IOException;
  
  int getResponseCode()
      throws IOException;
      
	String getResponseMessage()
      throws IOException;
      
	long getExpiration()
      throws IOException;
      
	long getDate()
      throws IOException;
      
  long getLastModified()
      throws IOException;
      
  String getHeaderField(String name)
      throws IOException;
      
  int getHeaderFieldInt(String name, int def)
      throws IOException;
      
  long getHeaderFieldDate(String name, long def)
      throws IOException;
	
	String getHeaderField(int n)
      throws IOException;
      
  String getHeaderFieldKey(int n)
      throws IOException;
      	
}
