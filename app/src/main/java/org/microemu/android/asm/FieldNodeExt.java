/**
 *  MicroEmulator
 *  Copyright (C) 2001-2010 Bartek Teodorczyk <barteo@barteo.net>
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
 *
 *  @version $Id: FieldNodeExt.java 1784 2008-10-20 14:22:57Z barteo $
 */

package org.microemu.android.asm;

import org.objectweb.asm.tree.FieldNode;

public class FieldNodeExt implements Comparable<FieldNodeExt> {

	FieldNode fieldNode;
	
	public FieldNodeExt(FieldNode fieldNode) {
		this.fieldNode = fieldNode;
	}

	public int compareTo(FieldNodeExt test) {
		int t = test.fieldNode.name.compareTo(fieldNode.name);
		if (t != 0) {
			return t;
		}

		t = test.fieldNode.desc.compareTo(fieldNode.desc);
		if (t != 0) {
			return t;
		}
		
		return 0;
	}

	@Override
	public String toString() {
		return fieldNode.access +"+"+ fieldNode.name +"+"+ fieldNode.desc +"+"+ fieldNode.signature +"+"+ fieldNode.value;
	}	

}
