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

package com.android.dx.cf.iface;

import com.android.dx.rop.type.Prototype;

/**
 * Interface representing methods of class files.
 */
public interface Method
    extends Member
{
    /**
     * Get the <i>effective</i> method descriptor, which includes, if
     * necessary, a first {@code this} parameter.
     *
     * @return {@code non-null;} the effective method descriptor
     */
    public Prototype getEffectiveDescriptor();
}
