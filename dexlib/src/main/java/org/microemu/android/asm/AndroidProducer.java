/**
 * MicroEmulator
 * Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2017-2018 Nikita Shakarun
 * Copyright (C) 2021-2022 Yury Kharchenko
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AndroidProducer {

	public static byte[] instrument(final byte[] classData, String classFileName)
			throws IllegalArgumentException {
		ClassReader cr = new ClassReader(classData);
		if (!cr.getClassName().equals(classFileName.substring(0, classFileName.length() - 6))) {
			throw new IllegalArgumentException("Class name does not match path");
		}

		// 自动重算 maxs/frames，防止校验失败
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		// 包装原有 AndroidClassVisitor，再加一层 FrameStripper
		ClassVisitor cv = new AndroidClassVisitor(cw) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc,
											 String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				return new MethodVisitor(Opcodes.ASM9, mv) {
					@Override
					public void visitFrame(int type, int nLocal, Object[] local,
										   int nStack, Object[] stack) {
						// 跳过所有 StackMapFrame，避免 dx 崩溃
						// 什么都不写
					}
				};
			}
		};

		// 用 SKIP_FRAMES 跳过 frame
		cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		return cw.toByteArray();
	}
}