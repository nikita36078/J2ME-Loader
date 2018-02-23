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

package com.android.dx.cf.attrib;

import com.android.dx.rop.cst.Constant;

/**
 * Attribute class for {@code AnnotationDefault} attributes.
 */
public final class AttAnnotationDefault extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "AnnotationDefault";

    /** {@code non-null;} the annotation default value */
    private final Constant value;

    /** {@code >= 0;} attribute data length in the original classfile (not
     * including the attribute header) */
    private final int byteLength;

    /**
     * Constructs an instance.
     *
     * @param value {@code non-null;} the annotation default value
     * @param byteLength {@code >= 0;} attribute data length in the original
     * classfile (not including the attribute header)
     */
    public AttAnnotationDefault(Constant value, int byteLength) {
        super(ATTRIBUTE_NAME);

        if (value == null) {
            throw new NullPointerException("value == null");
        }

        this.value = value;
        this.byteLength = byteLength;
    }

    /** {@inheritDoc} */
    @Override
    public int byteLength() {
        // Add six for the standard attribute header.
        return byteLength + 6;
    }

    /**
     * Gets the annotation default value.
     *
     * @return {@code non-null;} the value
     */
    public Constant getValue() {
        return value;
    }
}
