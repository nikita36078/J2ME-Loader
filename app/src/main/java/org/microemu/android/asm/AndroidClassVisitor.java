/**
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
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
 * @version $Id$
 */

package org.microemu.android.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.InsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class AndroidClassVisitor extends ClassVisitor {

	private static boolean enhanceCatchBlock = false;

	private boolean isMidlet;

	private String className;

	private HashMap<String, ArrayList<String>> classesHierarchy;

	private HashMap<String, TreeMap<FieldNodeExt, String>> fieldTranslations;

	private HashMap<String, ArrayList<String>> methodTranslations;

	private HashMap<Label, CatchInformation> catchInfo;

	private static class CatchInformation {

		Label label;

		String type;

		public CatchInformation(String type) {
			this.label = new Label();
			this.type = type;
		}
	}

	public class AndroidMethodVisitor extends PatternMethodAdapter {

		private final static int SEEN_NOTHING = 0;

		private final static int SEEN_I2B = 1;

		private int state;

		public AndroidMethodVisitor(MethodVisitor mv) {
			super(mv);
		}

		@Override
		protected void visitInsn() {
			state = SEEN_NOTHING;
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			if (isMidlet &&
					(opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC)) {

				String targetName = getTargetName(owner, name, desc);
				if (targetName != null) {
					mv.visitFieldInsn(opcode, owner, targetName, desc);
					return;
				}
			}

			super.visitFieldInsn(opcode, owner, name, desc);
		}

		private String getTargetName(String owner, String name, String desc) {
			ArrayList<String> classHierarchy = classesHierarchy.get(owner);
			if (classHierarchy != null) {
				for (int i = 0; i < classHierarchy.size(); i++) {
					String searchInClass = classHierarchy.get(i);
					TreeMap<FieldNodeExt, String> classFields = fieldTranslations.get(searchInClass);
					if (classFields != null) {
						String targetName = classFields.get(new FieldNodeExt(new FieldNode(-1, name, desc, null, null)));
						if (targetName != null) {
							//System.out.println("a1: " + owner +"+"+ searchInClass +"+"+ targetName);
							return targetName;
						}
					}
				}

				for (int i = 0; i < classHierarchy.size(); i++) {
					String searchInClass = classHierarchy.get(i);
					if (!owner.equals(searchInClass)) {
						String targetName = getTargetName(searchInClass, name, desc);
						if (targetName != null) {
							//System.out.println("a2: " + owner +"+"+ searchInClass +"+"+ name +"+"+ targetName);
							return targetName;
						}
					}
				}
			}

			return null;
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.BASTORE) {
				if (state != SEEN_I2B) {
					//System.out.println("I2B opcode needed !!!");
					//mv.visitInsn(Opcodes.I2B);
				}
			}
			visitInsn();
			if (opcode == Opcodes.I2B) {
				state = SEEN_I2B;
			}
			mv.visitInsn(opcode);
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			visitInsn();
			if (isMidlet) {
				if (opcode == Opcodes.INVOKEVIRTUAL) {
					if ((name.equals("getResourceAsStream")) && (owner.equals("java/lang/Class"))) {
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, "javax/microedition/util/ContextHolder", name, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;", itf);
						return;
					}
				}
				ArrayList<String> methods = methodTranslations.get(owner);
				if (methods != null && opcode == Opcodes.INVOKESPECIAL && methods.contains(name + desc) && owner.equals(className)) {
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, desc, itf);
					return;
				}
			}
			mv.visitMethodInsn(opcode, owner, name, desc, itf);
		}


		public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
			if (enhanceCatchBlock && type != null) {
				if (catchInfo == null) {
					catchInfo = new HashMap<>();
				}
				CatchInformation newHandler = catchInfo.get(handler);
				if (newHandler == null) {
					newHandler = new CatchInformation(type);
					catchInfo.put(handler, newHandler);
				}
				mv.visitTryCatchBlock(start, end, newHandler.label, type);
			} else {
				mv.visitTryCatchBlock(start, end, handler, type);
			}
		}
	}

	public class CheckCastRemover extends MethodNode {
		public CheckCastRemover(int access, final String name, String desc, final String signature, final String[] exceptions, MethodVisitor mv) {
			super(Opcodes.ASM5, access, name, desc, signature, exceptions);
			this.mv = mv;
		}

		public void visitEnd() {
			super.visitEnd();
			MethodNode node = (MethodNode) this;
			for (int i = 0; i < node.instructions.size()-1; i++) {
				MethodInsnNode invoke;
				TypeInsnNode cast;

				AbstractInsnNode insn = node.instructions.get(i);
				if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL)
					invoke = (MethodInsnNode) insn;
				else
					continue;

				insn = node.instructions.get(i+1);
				if (insn.getOpcode() == Opcodes.CHECKCAST)
					cast = (TypeInsnNode) insn;
				else
					continue;

				if (!invoke.owner.equals("javax/bluetooth/DataElement") || !invoke.name.equals("getValue")
						|| !cast.desc.equals("java/lang/String"))
					continue;

				// check for "LDC" instruction around
				// get the value and replace getValue() call with it
				// probably not a best solution, but faking
				// equals() is even harder
				int back = 1, forward = 1;
				String[] vals = new String[2];

				for (int j = i + forward; j < node.instructions.size(); j = i + forward++) {
					insn = node.instructions.get(j);
					if (insn.getOpcode() != Opcodes.LDC)
						continue;
					LdcInsnNode ldc = (LdcInsnNode) insn;
					if (!(ldc.cst instanceof String))
						continue;
					vals[0] = (String) ldc.cst;
					break;

				}

				for (int j = i - back; j > 0; j = i - back++) {
					insn = node.instructions.get(j);
					if (insn.getOpcode() != Opcodes.LDC)
						continue;
					LdcInsnNode ldc = (LdcInsnNode) insn;
					if (!(ldc.cst instanceof String))
						continue;
					vals[1] = (String) ldc.cst;
					break;
				}

				// get the nearest one
				String result = vals[(forward < back && vals[0] != null) ? 0 : 1];

				node.instructions.insertBefore(invoke, new InsnNode(Opcodes.POP));
				node.instructions.set(invoke, new LdcInsnNode(result));
				// node.instructions.insertBefore(cast, new InsnNode(Opcodes.NOP));
				node.instructions.remove(cast);


			}
			accept(mv);
		}
	}

	public AndroidClassVisitor(ClassVisitor cv, boolean isMidlet, HashMap<String, ArrayList<String>> classesHierarchy, HashMap<String, TreeMap<FieldNodeExt, String>> fieldTranslations,
							   HashMap<String, ArrayList<String>> methodTranslations) {
		super(Opcodes.ASM5, cv);

		this.isMidlet = isMidlet;
		this.classesHierarchy = classesHierarchy;
		this.fieldTranslations = fieldTranslations;
		this.methodTranslations = methodTranslations;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public MethodVisitor visitMethod(int access, final String name, String desc, final String signature, final String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new AndroidMethodVisitor(new CheckCastRemover(access, name, desc, signature, exceptions, mv));
	}
}
