/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.TargetInsn;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

/**
 * Instruction format {@code 22t}. See the instruction format spec
 * for details.
 */
public final class Form22t extends InsnFormat {
    /** {@code non-null;} unique instance of this class */
    public static final InsnFormat THE_ONE = new Form22t();

    /**
     * Constructs an instance. This class is not publicly
     * instantiable. Use {@link #THE_ONE}.
     */
    private Form22t() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() +
            ", " + branchString(insn);
    }

    /** {@inheritDoc} */
    @Override
    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return branchComment(insn);
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();

        if (!((insn instanceof TargetInsn) &&
              (regs.size() == 2) &&
              unsignedFitsInNibble(regs.get(0).getReg()) &&
              unsignedFitsInNibble(regs.get(1).getReg()))) {
            return false;
        }

        TargetInsn ti = (TargetInsn) insn;
        return ti.hasTargetOffset() ? branchFits(ti) : true;
    }

    /** {@inheritDoc} */
    @Override
    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(2);

        bits.set(0, unsignedFitsInNibble(regs.get(0).getReg()));
        bits.set(1, unsignedFitsInNibble(regs.get(1).getReg()));
        return bits;
    }

    /** {@inheritDoc} */
    @Override
    public boolean branchFits(TargetInsn insn) {
        int offset = insn.getTargetOffset();

        // Note: A zero offset would fit, but it is prohibited by the spec.
        return (offset != 0) && signedFitsInShort(offset);
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int offset = ((TargetInsn) insn).getTargetOffset();

        write(out,
              opcodeUnit(insn,
                         makeByte(regs.get(0).getReg(), regs.get(1).getReg())),
              (short) offset);
    }
}
