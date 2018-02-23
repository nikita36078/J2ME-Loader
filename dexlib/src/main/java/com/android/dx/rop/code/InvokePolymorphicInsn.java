/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.dx.rop.code;

import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

/**
 * An invoke-polymorphic instruction. This is a throwing instruction with
 * multiple constants.
 */
public class InvokePolymorphicInsn extends Insn {
    private static final CstString INVOKE_DESCRIPTOR =
            new CstString("([Ljava/lang/Object;)Ljava/lang/Object;");

    /** {@code non-null;} list of exceptions caught */
    private final TypeList catches;

    /**
     * {@code non-null;} method as it appears at the call site of the original
     * invoke-virtual instruction. This is used to construct the invoke method
     * to target and the call-site prototype.
     */
    private final CstMethodRef callSiteMethod;

    /**
     * {@code non-null;} method to invoke, either {@code java.lang.invoke.MethodHandle.invoke} or
     * {@code java.lang.invoke.MethodHandle.invokeExact}.
     */
    private final CstMethodRef invokeMethod;

    /**
     * {@code non-null;} the call site prototype.
     */
    private final CstProtoRef callSiteProto;

    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param sources {@code non-null;} specs for all the sources
     * @param catches {@code non-null;} list of exceptions caught
     * @param callSiteMethod {@code non-null;} the method called by
     * invoke-virtual that this instance will replace.
     */
    public InvokePolymorphicInsn(Rop opcode, SourcePosition position, RegisterSpecList sources, TypeList catches,
            CstMethodRef callSiteMethod) {
        super(opcode, position, null, sources);

        if (opcode.getBranchingness() != Rop.BRANCH_THROW) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }

        if (catches == null) {
            throw new NullPointerException("catches == null");
        }
        this.catches = catches;

        if (callSiteMethod == null) {
            throw new NullPointerException("callSiteMethod == null");
        } else if (!callSiteMethod.isSignaturePolymorphic()) {
            throw new IllegalArgumentException("callSiteMethod is not signature polymorphic");
        }

        this.callSiteMethod = callSiteMethod;
        this.invokeMethod = makeInvokeMethod(callSiteMethod);
        this.callSiteProto = makeCallSiteProto(callSiteMethod);
    }

    /** {@inheritDoc} */
    @Override
    public TypeList getCatches() {
        return this.catches;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(Visitor visitor) {
        visitor.visitInvokePolymorphicInsn(this);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withAddedCatch(Type type) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(),
                getSources(), catches.withAddedType(type), getCallSiteMethod());
    }

    /** {@inheritDoc} */
    @Override
    public Insn withRegisterOffset(int delta) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(),
                getSources().withOffset(delta),
                catches, getCallSiteMethod());
    }

    /** {@inheritDoc} */
    @Override
    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(),
                sources, catches, getCallSiteMethod());
    }

    /**
     * Gets the method as it appears at the call site of the original
     * invoke-virtual instruction.
     *
     * @return {@code non-null;} the original method reference
     */
    public CstMethodRef getCallSiteMethod() {
        return callSiteMethod;
    }

    /**
     * Gets the method to be invoked. This will be will either be
     * {@code java.lang.invoke.MethodHandle.invoke()} or
     * {@code java.lang.invoke.MethodHandle.invokeExact()}.
     *
     * @return {@code non-null;} method reference to be invoked
     */
    public CstMethodRef getInvokeMethod() {
        return invokeMethod;
    }

    /**
     * Gets the call site prototype. The call site prototype is provided
     * as an argument to invoke-polymorphic to enable type checking and
     * type conversion.
     *
     * @return {@code non-null;} Prototype reference for call site
     */
    public CstProtoRef getCallSiteProto() {
        return callSiteProto;
    }

    /** {@inheritDoc} */
    @Override
    public String getInlineString() {
        return getInvokeMethod().toString() + " " +
            getCallSiteProto().toString() + " " +
            ThrowingInsn.toCatchString(catches);
    }

    private static CstMethodRef makeInvokeMethod(final CstMethodRef callSiteMethod) {
        // The name is either invoke or invokeExact. The INVOKE_DESCRIPTOR is fixed.
        CstNat cstNat = new CstNat(callSiteMethod.getNat().getName(), INVOKE_DESCRIPTOR);
        return new CstMethodRef(CstType.METHOD_HANDLE, cstNat);
    }

    private static CstProtoRef makeCallSiteProto(final CstMethodRef callSiteMethod) {
        return new CstProtoRef(callSiteMethod.getPrototype(true));
    }
}
