/*
 *  Copyright 2021 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.microemu.android.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

public class TimerTaskRunPatcher extends AndroidMethodVisitor {

	private Label labelTryEnd;
	private Label labelCatchStart;
	private Label labelCatchEnd;

	TimerTaskRunPatcher(MethodVisitor mv) {
		super(mv);
	}

	@Override
	public void visitCode() {
		super.visitCode();
		Label labelTryStart = new Label();
		labelTryEnd = new Label();
		labelCatchStart = new Label();
		labelCatchEnd = new Label();
		mv.visitTryCatchBlock(labelTryStart, labelTryEnd, labelCatchStart, "java/lang/Exception");
		mv.visitLabel(labelTryStart);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitLabel(labelTryEnd);
		mv.visitJumpInsn(GOTO, labelCatchEnd);
		mv.visitLabel(labelCatchStart);
		mv.visitVarInsn(ASTORE, 1);
		if (USE_PANIC_LOGGING) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
		}
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, AndroidClassVisitor.TIMER_TASK, "cancel", "()Z", false);
		mv.visitInsn(POP);
		mv.visitLabel(labelCatchEnd);
		mv.visitInsn(RETURN);

		mv.visitMaxs(maxStack, maxLocals);
	}
}
