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

package com.android.dx.rop.cst;

/**
 * Constants of type {@code MethodHandle}.
 */
public final class CstMethodHandle extends Constant {

    public static final int KIND_GETFIELD = 1;
    public static final int KIND_GETSTATIC = 2;
    public static final int KIND_PUTFIELD = 3;
    public static final int KIND_PUTSTATIC = 4;
    public static final int KIND_INVOKEVIRTUAL = 5;
    public static final int KIND_INVOKESTATIC = 6;
    public static final int KIND_INVOKESPECIAL = 7;
    public static final int KIND_NEWINVOKESPECIAL = 8;
    public static final int KIND_INVOKEINTERFACE = 9;

    /** The kind of MethodHandle */
    private int kind;
    /** {@code non-null;} the referenced constant */
    private final Constant ref;


    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param kind the kind of this handle
     * @param ref the actual referenced constant
     * @return {@code non-null;} the appropriate instance
     */
    public static CstMethodHandle make(int kind, Constant ref) {
        return new CstMethodHandle(kind, ref);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param kind the kind of this handle
     * @param ref the actual referenced constant
     */
    private CstMethodHandle(int kind, Constant ref) {
        this.kind = kind;
        this.ref = ref;
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "method handle";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return toString();
    }

    /**
     * Gets the actual constant.
     *
     * @return the value
     */
    public Constant getRef() {
        return ref;
    }

    /**
     * Gets the kind of this method handle.
     *
     * @return the kind
     */
    public int getKind() {
        return kind;
    }

    @Override
    public boolean isCategory2() {
        return false;
    }

    @Override
    protected int compareTo0(Constant other) {
        CstMethodHandle otherHandle = (CstMethodHandle) other;
        if (getKind() == otherHandle.getKind()) {
            return getRef().compareTo(otherHandle.getRef());
        } else {
            return Integer.compare(getKind(), otherHandle.getKind());
        }
    }
}
