/**
 * MicroEmulator
 * Copyright (C) 2001-2010 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2017 Nikita Shakarun
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 *
 * @version $Id: FirstPassVisitor.java 1784 2008-10-20 14:22:57Z barteo $
 */

package org.microemu.android.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;

public class FirstPassVisitor extends ClassVisitor {
	private HashMap<String, ArrayList<String>> classesHierarchy;
	private HashMap<String, ArrayList<String>> methodTranslations;
	private String name;
	private ArrayList<String> methods = new ArrayList<>();

	public FirstPassVisitor(HashMap<String, ArrayList<String>> classesHierarchy, HashMap<String, ArrayList<String>> methodTranslations) {
		super(Opcodes.ASM5);
		this.classesHierarchy = classesHierarchy;
		this.methodTranslations = methodTranslations;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		ArrayList<String> list = new ArrayList<>();
		this.name = name;
		list.add(name);
		list.add(superName);
		classesHierarchy.put(name, list);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null;
	}

	@Override
	public void visitAttribute(Attribute attr) {
	}

	@Override
	public void visitEnd() {
		methodTranslations.put(name, methods);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return null;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, String desc, final String signature, final String[] exceptions) {
		if (!name.equals("<init>") && (access & Opcodes.ACC_PRIVATE) == 0) {
			methods.add(name + desc);
		}
		return null;
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
	}

	@Override
	public void visitSource(String source, String debug) {
	}

}
