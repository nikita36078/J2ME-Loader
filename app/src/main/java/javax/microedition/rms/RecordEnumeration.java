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
 
package javax.microedition.rms;


public interface RecordEnumeration
{

  int numRecords();
  
  byte[] nextRecord()
      throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException;
  
  int nextRecordId()
      throws InvalidRecordIDException;
  
  byte[] previousRecord()
      throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException;
  
  int previousRecordId()
      throws InvalidRecordIDException;
  
  boolean hasNextElement();
  
  boolean hasPreviousElement();
  
  void reset();
  
  void rebuild();
  
  void keepUpdated(boolean keepUpdated);
  
  boolean isKeptUpdated();
  
  void destroy();

}

