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
 * Constants of type {@code InvokeDynamic}.
 */
public final class CstInvokeDynamic extends Constant {

    /** The index of the bootstrap method in the bootstrap method table */
    private final int bootstrapMethodIndex;
    /** {@code non-null;} the name and type */
    private final CstNat nat;


    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param bootstrapMethodIndex The index of the bootstrap method in the bootstrap method table
     * @param nat the name and type
     * @return {@code non-null;} the appropriate instance
     */
    public static CstInvokeDynamic make(int bootstrapMethodIndex, CstNat nat) {
        return new CstInvokeDynamic(bootstrapMethodIndex, nat);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param bootstrapMethodIndex The index of the bootstrap method in the bootstrap method table
     * @param nat the name and type
     */
    private CstInvokeDynamic(int bootstrapMethodIndex, CstNat nat) {
        this.bootstrapMethodIndex = bootstrapMethodIndex;
        this.nat = nat;
    }

    @Override
    public String toString() {
        return toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "InvokeDynamic";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return "InvokeDynamic(" + bootstrapMethodIndex + ", " + nat.toHuman() + ")";
    }

    /**
     * Gets the bootstrap method index.
     *
     * @return the bootstrap method index
     */
    public int getBootstrapMethodIndex() {
      return bootstrapMethodIndex;
    }

    /**
     * Gets the {@code CstNat} value.
     *
     * @return the name and type
     */
    public CstNat getNat() {
      return nat;
    }

    @Override
    public boolean isCategory2() {
        return false;
    }

    @Override
    protected int compareTo0(Constant other) {
        CstInvokeDynamic otherInvoke = (CstInvokeDynamic) other;
        if (bootstrapMethodIndex == otherInvoke.getBootstrapMethodIndex()) {
            return nat.compareTo(otherInvoke.getNat());
        } else {
            return Integer.compare(bootstrapMethodIndex, otherInvoke.getBootstrapMethodIndex());
        }
    }
}
