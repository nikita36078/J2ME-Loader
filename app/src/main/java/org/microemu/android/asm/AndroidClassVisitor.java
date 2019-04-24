/**
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2017-2018 Nikita Shakarun
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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AndroidClassVisitor extends ClassVisitor {

	private String encoding;

	public class AndroidMethodVisitor extends PatternMethodAdapter {

		private final static int SEEN_NOTHING = 0;

		private final static int SEEN_I2B = 1;

		private int state;

		private boolean enhanceCatchBlock = false;

		private Label exceptionHandler;

		public AndroidMethodVisitor(MethodVisitor mv) {
			super(mv);
		}

		@Override
		protected void visitInsn() {
			state = SEEN_NOTHING;
		}

		@Override
		public void visitInsn(int opcode) {
			visitInsn();
			if (opcode == Opcodes.I2B) {
				state = SEEN_I2B;
			}
			mv.visitInsn(opcode);
		}

		@Override
		public void visitLabel(Label label) {
			mv.visitLabel(label);
			if (enhanceCatchBlock && label == exceptionHandler) {
				mv.visitInsn(Opcodes.DUP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
				exceptionHandler = null;
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			visitInsn();
			if (owner.equals("java/lang/Class")) {
				if (name.equals("getResourceAsStream")) {
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "javax/microedition/util/ContextHolder", name,
							"(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;", itf);
					return;
				}
			} else if (owner.equals("java/lang/String")) {
				if (name.equals("<init>") && desc.startsWith("([B") && !desc.endsWith("Ljava/lang/String;)V")) {
					mv.visitLdcInsn(encoding);
					mv.visitMethodInsn(opcode, owner, name, new StringBuffer()
							.append(desc, 0, desc.length() - 2)
							.append("Ljava/lang/String;)V").toString(), itf);
					return;
				} else if (name.equals("getBytes") && desc.startsWith("()")) {
					mv.visitLdcInsn(encoding);
					mv.visitMethodInsn(opcode, owner, name, "(Ljava/lang/String;)[B", itf);
					return;
				}
			}
			mv.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		@Override
		public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
			if (enhanceCatchBlock && type != null) {
				exceptionHandler = handler;
			}
			mv.visitTryCatchBlock(start, end, handler, type);
		}

	}

	public AndroidClassVisitor(ClassVisitor cv, String encoding) {
		super(Opcodes.ASM7, cv);
		this.encoding = encoding;
	}

	@Override
	public MethodVisitor visitMethod(int access, final String name, String desc, final String signature, final String[] exceptions) {
		return new AndroidMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
	}

}
