package org.microemu.android.asm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_5;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodRenamerTest {
	private static final String THREAD = "test/MyThread";
	private static final String SUB_THREAD = "test/MySubThread";
	private static final String CALLER = "test/Caller";
	private static final String OTHER = "test/Other";

	@Test
	public void renamesThreadFinalMethodsAndCallSites() {
		MethodRenamer renamer = buildRenamer();

		byte[] thread = AndroidProducer.instrument(
				createThreadClass(THREAD, "java/lang/Thread"), THREAD + ".class", renamer);
		Set<String> methods = methodNames(thread);
		assertTrue(methods.contains("resume$j2me()V"));
		assertTrue(methods.contains("suspend$j2me()V"));
		assertFalse(methods.contains("resume()V"));
		assertFalse(methods.contains("suspend()V"));
		assertTrue(methods.contains("run()V"));

		assertEquals("resume$j2me", renamer.getRenamedMethod(SUB_THREAD, "resume", "()V"));
		assertEquals("suspend$j2me", renamer.getRenamedMethod(SUB_THREAD, "suspend", "()V"));

		byte[] caller = AndroidProducer.instrument(createCallerClass(), CALLER + ".class", renamer);
		assertEquals(
				Arrays.asList(THREAD + ".resume$j2me()V", THREAD + ".suspend$j2me()V"),
				methodCalls(caller, "call"));
		assertEquals(
				Collections.singletonList(SUB_THREAD + ".resume$j2me()V"),
				methodCalls(caller, "callSub"));
		assertEquals(
				Collections.singletonList(OTHER + ".resume()V"),
				methodCalls(caller, "callOther"));
	}

	@Test
	public void doesNotRenameMethodsOfNonThreadClass() {
		MethodRenamer renamer = buildRenamer();
		byte[] other = AndroidProducer.instrument(createOtherClass(), OTHER + ".class", renamer);
		assertTrue(methodNames(other).contains("resume()V"));
		assertFalse(methodNames(other).contains("resume$j2me()V"));
	}

	private static MethodRenamer buildRenamer() {
		MethodRenamer.Builder builder = new MethodRenamer.Builder();
		builder.addClass(createThreadClass(THREAD, "java/lang/Thread"));
		builder.addClass(createSubThreadClass());
		builder.addClass(createCallerClass());
		builder.addClass(createOtherClass());
		return builder.build();
	}

	private static byte[] createThreadClass(String name, String superName) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(V1_5, ACC_PUBLIC, name, null, superName, null);
		addConstructor(writer, superName);
		addVoidMethod(writer, "resume");
		addVoidMethod(writer, "suspend");
		addVoidMethod(writer, "run");
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] createSubThreadClass() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(V1_5, ACC_PUBLIC, SUB_THREAD, null, THREAD, null);
		addConstructor(writer, THREAD);
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] createOtherClass() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(V1_5, ACC_PUBLIC, OTHER, null, "java/lang/Object", null);
		addConstructor(writer, "java/lang/Object");
		addVoidMethod(writer, "resume");
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] createCallerClass() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(V1_5, ACC_PUBLIC, CALLER, null, "java/lang/Object", null);
		addConstructor(writer, "java/lang/Object");
		addCallMethod(writer, "call", THREAD, true);
		addCallMethod(writer, "callSub", SUB_THREAD, false);
		addCallMethod(writer, "callOther", OTHER, false);
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static void addConstructor(ClassWriter writer, String superName) {
		MethodVisitor method = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		method.visitCode();
		method.visitVarInsn(ALOAD, 0);
		method.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
		method.visitInsn(RETURN);
		method.visitMaxs(0, 0);
		method.visitEnd();
	}

	private static void addVoidMethod(ClassWriter writer, String name) {
		MethodVisitor method = writer.visitMethod(ACC_PUBLIC, name, "()V", null, null);
		method.visitCode();
		method.visitInsn(RETURN);
		method.visitMaxs(0, 0);
		method.visitEnd();
	}

	private static void addCallMethod(ClassWriter writer, String name, String owner, boolean suspend) {
		MethodVisitor method = writer.visitMethod(
				ACC_PUBLIC | ACC_STATIC, name, "(L" + owner + ";)V", null, null);
		method.visitCode();
		method.visitVarInsn(ALOAD, 0);
		method.visitMethodInsn(INVOKEVIRTUAL, owner, "resume", "()V", false);
		if (suspend) {
			method.visitVarInsn(ALOAD, 0);
			method.visitMethodInsn(INVOKEVIRTUAL, owner, "suspend", "()V", false);
		}
		method.visitInsn(RETURN);
		method.visitMaxs(0, 0);
		method.visitEnd();
	}

	private static Set<String> methodNames(byte[] bytecode) {
		Set<String> names = new HashSet<>();
		new ClassReader(bytecode).accept(new ClassVisitor(ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor,
											 String signature, String[] exceptions) {
				names.add(name + descriptor);
				return null;
			}
		}, ClassReader.SKIP_CODE);
		return names;
	}

	private static List<String> methodCalls(byte[] bytecode, String methodName) {
		List<String> calls = new ArrayList<>();
		new ClassReader(bytecode).accept(new ClassVisitor(ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor,
											 String signature, String[] exceptions) {
				if (!methodName.equals(name)) {
					return null;
				}
				return new MethodVisitor(ASM9) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name,
												String descriptor, boolean isInterface) {
						calls.add(owner + "." + name + descriptor);
					}
				};
			}
		}, 0);
		return calls;
	}
}
