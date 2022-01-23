/**
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2017-2018 Nikita Shakarun
 * Copyright 2020-2022 Yury Kharchenko
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
 * @version $Id$
 */

package org.microemu.android.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;

import static org.objectweb.asm.Opcodes.*;

public class AndroidMethodVisitor extends MethodVisitor {
	static boolean USE_PANIC_LOGGING = false;
	private final ArrayList<Label> exceptionHandlers = new ArrayList<>();

	public AndroidMethodVisitor(MethodVisitor methodVisitor) {
		super(ASM9, methodVisitor);
	}

	@Override
	public void visitLabel(Label label) {
		mv.visitLabel(label);
		if (USE_PANIC_LOGGING && exceptionHandlers.contains(label)) {
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		switch (owner) {
			case "java/lang/Class":
				if (name.equals("getResourceAsStream")) {
					mv.visitMethodInsn(INVOKESTATIC, "javax/microedition/util/ContextHolder",
							name, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;", itf);
					return;
				}
				break;
			case "java/lang/String":
				if (name.equals("<init>") && desc.startsWith("([B") && !desc.endsWith("Ljava/lang/String;)V")) {
					injectGetPropertyEncoding();
					String descriptor = new StringBuilder(desc.length() + 18)
							.append(desc)
							.insert(desc.length() - 2, "Ljava/lang/String;")
							.toString();
					mv.visitMethodInsn(opcode, owner, name, descriptor, itf);
					return;
				} else if (name.equals("getBytes"))
					if (desc.equals("()[B")) {
						injectGetPropertyEncoding();
						mv.visitMethodInsn(opcode, owner, name, "(Ljava/lang/String;)[B", itf);
						return;
					}
				break;
			case "java/io/InputStreamReader":
				if (name.equals("<init>") && desc.equals("(Ljava/io/InputStream;)V")) {
					injectGetPropertyEncoding();
					mv.visitMethodInsn(opcode, owner, name, "(Ljava/io/InputStream;Ljava/lang/String;)V", itf);
					return;
				}
				break;
			case "java/io/OutputStreamWriter":
				if (name.equals("<init>") && desc.equals("(Ljava/io/OutputStream;)V")) {
					injectGetPropertyEncoding();
					mv.visitMethodInsn(opcode, owner, name, "(Ljava/io/OutputStream;Ljava/lang/String;)V", itf);
					return;
				}
				break;
			case "java/io/ByteArrayOutputStream":
				if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
					injectGetPropertyEncoding();
					mv.visitMethodInsn(opcode, owner, name, "(Ljava/lang/String;)Ljava/lang/String;", itf);
					return;
				}
				break;
			case "java/io/PrintStream":
				if (name.equals("<init>") && desc.equals("(Ljava/io/OutputStream;)V")) {
					mv.visitInsn(ICONST_0);
					injectGetPropertyEncoding();
					mv.visitMethodInsn(opcode, owner, name, "(Ljava/io/OutputStream;ZLjava/lang/String;)V", itf);
					return;
				}
				break;
			case "com/siemens/mp/io/Connection":
				if (opcode == INVOKESTATIC && name.equals("setListener")) {
					name = "setListenerCompat";
				}
				break;
			case "java/lang/System":
				if (opcode == INVOKESTATIC && name.equals("getProperty")) {
					mv.visitMethodInsn(opcode, "javax/microedition/shell/MidletSystem", name, desc, itf);
					return;
				}
				break;
		}
		mv.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	private void injectGetPropertyEncoding() {
		mv.visitLdcInsn("microedition.encoding");
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty",
				"(Ljava/lang/String;)Ljava/lang/String;", false);
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
		if (USE_PANIC_LOGGING && type != null) {
			exceptionHandlers.add(handler);
		}
		mv.visitTryCatchBlock(start, end, handler, type);
	}
}
