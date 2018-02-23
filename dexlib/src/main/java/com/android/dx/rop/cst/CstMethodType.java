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

import com.android.dx.rop.type.Prototype;

/**
 * Constants of type {@code MethodType}.
 */
public final class CstMethodType extends Constant {
    /** {@code non-null;} the raw prototype for this method */
    private final Prototype prototype;

    /**
     * Makes an instance for the given value. This may (but does not
     * necessarily) return an already-allocated instance.
     *
     * @param descriptor the method descriptor
     * @return {@code non-null;} the appropriate instance
     */
    public static CstMethodType make(CstString descriptor) {
        return new CstMethodType(descriptor);
    }

    /**
     * Constructs an instance. This constructor is private; use {@link #make}.
     *
     * @param descriptor the method descriptor
     */
    private CstMethodType(CstString descriptor) {
        prototype = Prototype.fromDescriptor(descriptor.getString());
    }

    @Override
    public String toString() {
        return prototype.getDescriptor();
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "method type";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return toString();
    }

    /**
     * Gets the {@code Prototype} value.
     *
     * @return the value
     */
    public Prototype getValue() {
        return prototype;
    }

    @Override
    public boolean isCategory2() {
        return false;
    }

    @Override
    protected int compareTo0(Constant other) {
        return prototype.getDescriptor().compareTo(
            ((CstMethodType) other).getValue().getDescriptor());
    }
}
