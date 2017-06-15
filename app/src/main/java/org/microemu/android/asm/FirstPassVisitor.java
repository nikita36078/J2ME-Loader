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
 *  @version $Id: FirstPassVisitor.java 1784 2008-10-20 14:22:57Z barteo $
 */

package org.microemu.android.asm;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.*;

public class FirstPassVisitor extends ClassVisitor
{
	private HashMap<String, ArrayList<String>> classesHierarchy;
	private HashMap<String, ArrayList<String>> methodTranslations;
	private String name;
	private ArrayList<String> methods = new ArrayList<>();

	public FirstPassVisitor(HashMap<String, ArrayList<String>> classesHierarchy, HashMap<String, ArrayList<String>> methodTranslations)
	{
		super(Opcodes.ASM5);
		this.classesHierarchy = classesHierarchy;
		this.methodTranslations = methodTranslations;
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
	{		
//		System.out.println("class: " + name +" + "+ superName);
		ArrayList<String> list = new ArrayList<String>();
		this.name = name;
		list.add(name);
		list.add(superName);
		classesHierarchy.put(name, list);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void visitAttribute(Attribute attr)
	{
		// TODO Auto-generated method stub

	}

	public void visitEnd()
	{
		methodTranslations.put(name, methods);
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
	{
		return null;
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access)
	{
		// TODO Auto-generated method stub

	}

	public MethodVisitor visitMethod(final int access, final String name, String desc, final String signature, final String[] exceptions)
	{
		if(!name.equals("<init>") && (access & Opcodes.ACC_PRIVATE) == 0){
			methods.add(name + desc);
		}
		return null;
	}

	public void visitOuterClass(String owner, String name, String desc)
	{
		// TODO Auto-generated method stub

	}

	public void visitSource(String source, String debug)
	{
		// TODO Auto-generated method stub

	}

}
