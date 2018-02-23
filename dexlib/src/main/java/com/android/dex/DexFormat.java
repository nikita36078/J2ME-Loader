/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dex;

/**
 * Constants that show up in and are otherwise related to {@code .dex}
 * files, and helper methods for same.
 */
public final class DexFormat {
    private DexFormat() {}

    /** API level to target in order to generate invoke-polymorphic */
    public static final int API_INVOKE_POLYMORPHIC = 26;

    /** API level to target in order to pass through default and static interface methods */
    public static final int API_DEFAULT_INTERFACE_METHODS = 24;

    /** API level to target in order to suppress extended opcode usage */
    public static final int API_NO_EXTENDED_OPCODES = 13;

    /**
     * API level to target in order to produce the most modern file
     * format
     */
    public static final int API_CURRENT = API_INVOKE_POLYMORPHIC;

    /** dex file version number for API level 26 and earlier */
    public static final String VERSION_FOR_API_26 = "038";

    /** dex file version number for API level 24 and earlier */
    public static final String VERSION_FOR_API_24 = "037";

    /** dex file version number for API level 13 and earlier */
    public static final String VERSION_FOR_API_13 = "035";

    /**
     * Dex file version number for dalvik.
     * <p>
     * Note: Dex version 36 was loadable in some versions of Dalvik but was never fully supported or
     * completed and is not considered a valid dex file format.
     * </p>
     */
    public static final String VERSION_CURRENT = VERSION_FOR_API_26;

    /**
     * file name of the primary {@code .dex} file inside an
     * application or library {@code .jar} file
     */
    public static final String DEX_IN_JAR_NAME = "classes.dex";

    /** common prefix for all dex file "magic numbers" */
    public static final String MAGIC_PREFIX = "dex\n";

    /** common suffix for all dex file "magic numbers" */
    public static final String MAGIC_SUFFIX = "\0";

    /**
     * value used to indicate endianness of file contents
     */
    public static final int ENDIAN_TAG = 0x12345678;

    /**
     * Maximum addressable field or method index.
     * The largest addressable member is 0xffff, in the "instruction formats" spec as field@CCCC or
     * meth@CCCC.
     */
    public static final int MAX_MEMBER_IDX = 0xFFFF;

    /**
     * Maximum addressable type index.
     * The largest addressable type is 0xffff, in the "instruction formats" spec as type@CCCC.
     */
    public static final int MAX_TYPE_IDX = 0xFFFF;

    /**
     * Returns the API level corresponding to the given magic number,
     * or {@code -1} if the given array is not a well-formed dex file
     * magic number.
     */
    public static int magicToApi(byte[] magic) {
        if (magic.length != 8) {
            return -1;
        }

        if ((magic[0] != 'd') || (magic[1] != 'e') || (magic[2] != 'x') || (magic[3] != '\n') ||
                (magic[7] != '\0')) {
            return -1;
        }

        String version = "" + ((char) magic[4]) + ((char) magic[5]) +((char) magic[6]);

        if (version.equals(VERSION_FOR_API_13)) {
            return API_NO_EXTENDED_OPCODES;
        } else if (version.equals(VERSION_FOR_API_24)) {
            return API_DEFAULT_INTERFACE_METHODS;
        } else if (version.equals(VERSION_FOR_API_26)) {
            return API_INVOKE_POLYMORPHIC;
        } else if (version.equals(VERSION_CURRENT)) {
            return API_CURRENT;
        }

        return -1;
    }

    /**
     * Returns the magic number corresponding to the given target API level.
     */
    public static String apiToMagic(int targetApiLevel) {
        String version;

        if (targetApiLevel >= API_CURRENT) {
            version = VERSION_CURRENT;
        } else if (targetApiLevel >= API_INVOKE_POLYMORPHIC) {
            version = VERSION_FOR_API_26;
        } else if (targetApiLevel >= API_DEFAULT_INTERFACE_METHODS) {
            version = VERSION_FOR_API_24;
        } else {
            version = VERSION_FOR_API_13;
        }

        return MAGIC_PREFIX + version + MAGIC_SUFFIX;
    }

    public static boolean isSupportedDexMagic(byte[] magic) {
        int api = magicToApi(magic);
        return api > 0;
    }
}
