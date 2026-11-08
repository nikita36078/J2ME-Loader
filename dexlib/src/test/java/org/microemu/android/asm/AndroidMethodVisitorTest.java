package org.microemu.android.asm;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.V1_5;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AndroidMethodVisitorTest {
	private static final String FIXTURE_NAME = "org/microemu/android/asm/BooleanReturnFixture";

	@Test
	public void booleanIreturnFromShortFieldIsNarrowedToJvmSemantics() throws Exception {
		byte[] transformed = transform(createFixture());

		assertEquals(
				Arrays.asList(ICONST_1, IAND, IRETURN),
				instructionOpcodes(transformed, "fromShortField", "()Z"));

		Class<?> fixture = new FixtureClassLoader().define(transformed);
		Field value = fixture.getField("value");
		Method fromShortField = fixture.getMethod("fromShortField");
		assertBooleanValue(value, fromShortField, (short) 0, false);
		assertBooleanValue(value, fromShortField, (short) 1, true);
		assertBooleanValue(value, fromShortField, (short) 2, false);
		assertBooleanValue(value, fromShortField, (short) 3, true);
		assertBooleanValue(value, fromShortField, (short) -1, true);
	}

	@Test
	public void nonBooleanIreturnIsNotRewritten() {
		byte[] transformed = transform(createFixture());

		assertEquals(
				Arrays.asList(IRETURN),
				instructionOpcodes(transformed, "asInt", "()I"));
	}

	private static void assertBooleanValue(Field field, Method method, short value, boolean expected) throws Exception {
		field.setShort(null, value);
		assertEquals(Boolean.valueOf(expected), method.invoke(null));
	}

	private static byte[] createFixture() {
		ClassWriter writer = new ClassWriter(0);
		writer.visit(V1_5, ACC_PUBLIC, FIXTURE_NAME, null, "java/lang/Object", null);
		FieldVisitor field = writer.visitField(ACC_PUBLIC | ACC_STATIC, "value", "S", null, null);
		field.visitEnd();
		writeShortFieldReturnMethod(writer, "fromShortField", "()Z");
		writeShortFieldReturnMethod(writer, "asInt", "()I");
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static void writeShortFieldReturnMethod(ClassWriter writer, String name, String descriptor) {
		MethodVisitor method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, name, descriptor, null, null);
		method.visitCode();
		method.visitFieldInsn(GETSTATIC, FIXTURE_NAME, "value", "S");
		method.visitInsn(IRETURN);
		method.visitMaxs(1, 0);
		method.visitEnd();
	}

	private static byte[] transform(byte[] input) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		new ClassReader(input).accept(new AndroidClassVisitor(writer), 0);
		return writer.toByteArray();
	}

	private static List<Integer> instructionOpcodes(byte[] bytecode, String methodName, String descriptor) {
		List<Integer> opcodes = new ArrayList<>();
		new ClassReader(bytecode).accept(new ClassVisitor(ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (!methodName.equals(name) || !descriptor.equals(desc)) {
					return null;
				}
				return new MethodVisitor(ASM9) {
					@Override
					public void visitInsn(int opcode) {
						opcodes.add(opcode);
					}
				};
			}
		}, 0);
		return opcodes;
	}

	private static final class FixtureClassLoader extends ClassLoader {
		Class<?> define(byte[] bytecode) {
			return defineClass(FIXTURE_NAME.replace('/', '.'), bytecode, 0, bytecode.length);
		}
	}
}