/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.dx.dex.code;

import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Constructor of {@link CatchTable} instances from {@link RopMethod}
 * and associated data.
 *
 * <p>The ROP-level information needed to build the table (block throw
 * points, handler targets and catch types) is captured in the constructor,
 * so the whole ROP method graph becomes collectable right after translation,
 * instead of staying reachable through this instance until the dex file is written.</p>
 */
public final class StdCatchBuilder implements CatchBuilder {
    /** the maximum range of a single catch handler, in code units */
    private static final int MAX_CATCH_RANGE = 65535;

    /** empty block list, used when the method has no catch handlers */
    private static final ThrowingBlock[] EMPTY = new ThrowingBlock[0];

    /** {@code non-null;} blocks that can throw, in output order */
    private final ThrowingBlock[] blocks;

    /** whether the method has any catch handlers at all */
    private final boolean hasAnyCatches;

    /** {@code non-null;} all catch types used by the method */
    private final HashSet<Type> catchTypes;

    /**
     * Captured data for a single block that can throw. Addresses are resolved
     * when the table is built, after the Dalv instruction list has been laid
     * out.
     */
    private static final class ThrowingBlock {
        /** {@code non-null;} address of the last (throwing) instruction */
        final CodeAddress start;

        /** {@code non-null;} address just past the end of the block */
        final CodeAddress end;

        /** {@code null-ok;} handler types, or {@code null} if none */
        final CstType[] types;

        /** {@code null-ok;} handler target addresses, or {@code null} if none */
        final CodeAddress[] handlers;

        private ThrowingBlock(CodeAddress start, CodeAddress end,
                CstType[] types, CodeAddress[] handlers) {
            this.start = start;
            this.end = end;
            this.types = types;
            this.handlers = handlers;
        }

        /**
         * Makes the {@link CatchHandlerList} for this block. Addresses are
         * resolved here, after the Dalv instruction list has been laid out.
         *
         * @return {@code non-null;} the handler list
         */
        private CatchHandlerList makeHandlers() {
            if (types == null) {
                return CatchHandlerList.EMPTY;
            }

            CatchHandlerList result = new CatchHandlerList(types.length);
            for (int i = 0; i < types.length; i++) {
                result.set(i, types[i], handlers[i].getAddress());
            }
            result.setImmutable();
            return result;
        }
    }

    /**
     * Constructs an instance. It captures the data needed to build the catch
     * table and does not retain the ROP method.
     *
     * @param method {@code non-null;} the original method
     * @param order {@code non-null;} block output order
     * @param addresses {@code non-null;} address objects for each block
     */
    public StdCatchBuilder(RopMethod method, int[] order,
            BlockAddresses addresses) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        if (order == null) {
            throw new NullPointerException("order == null");
        }

        if (addresses == null) {
            throw new NullPointerException("addresses == null");
        }

        BasicBlockList ropBlocks = method.getBlocks();
        int len = order.length;
        ArrayList<ThrowingBlock> list = new ArrayList<>(len);
        HashSet<Type> types = new HashSet<>(20);
        boolean any = false;

        for (int i = 0; i < len; i++) {
            BasicBlock block = ropBlocks.labelToBlock(order[i]);
            TypeList catches = block.getLastInsn().getCatches();
            int catchSize = catches.size();

            /*
             * All declared types take part in catch type interning, even
             * ones after a catch-all (which shouldn't normally occur).
             */
            for (int j = 0; j < catchSize; j++) {
                types.add(catches.getType(j));
            }

            if (catchSize != 0) {
                any = true;
            }

            if (!block.canThrow()) {
                /*
                 * There is no need to concern ourselves with the
                 * placement of blocks that can't throw with respect
                 * to the blocks that *can* throw.
                 */
                continue;
            }

            IntList successors = block.getSuccessors();
            CstType[] cstTypes = null;
            CodeAddress[] handlerAddrs = null;

            if (catchSize != 0) {
                int primary = block.getPrimarySuccessor();

                if (((primary == -1) && (successors.size() != catchSize))
                        || ((primary != -1) &&
                                ((successors.size() != (catchSize + 1))
                                        || (primary != successors.get(catchSize))))) {
                    /*
                     * Blocks that throw are supposed to list their primary
                     * successor -- if any -- last in the successors list, but
                     * that constraint appears to be violated here.
                     */
                    throw new RuntimeException(
                            "shouldn't happen: weird successors list");
                }

                /*
                 * Reduce the effective catchSize if we spot a catch-all that
                 * isn't at the end.
                 */
                for (int j = 0; j < catchSize; j++) {
                    Type type = catches.getType(j);
                    if (type.equals(Type.OBJECT)) {
                        catchSize = j + 1;
                        break;
                    }
                }

                cstTypes = new CstType[catchSize];
                handlerAddrs = new CodeAddress[catchSize];

                for (int j = 0; j < catchSize; j++) {
                    cstTypes[j] = new CstType(catches.getType(j));
                    handlerAddrs[j] = addresses.getStart(successors.get(j));
                }
            }

            list.add(new ThrowingBlock(addresses.getLast(block),
                    addresses.getEnd(block), cstTypes, handlerAddrs));
        }

        this.blocks = any ? list.toArray(new ThrowingBlock[list.size()]) : EMPTY;
        this.hasAnyCatches = any;
        this.catchTypes = types;
    }

    /** {@inheritDoc} */
    @Override
    public CatchTable build() {
        int len = blocks.length;
        ArrayList<CatchTable.Entry> resultList = new ArrayList<>(len);
        CatchHandlerList currentHandlers = CatchHandlerList.EMPTY;
        ThrowingBlock currentStart = null;
        ThrowingBlock currentEnd = null;

        for (int i = 0; i < len; i++) {
            ThrowingBlock block = blocks[i];
            CatchHandlerList handlers = block.makeHandlers();

            if (currentHandlers.size() == 0) {
                // This is the start of a new catch range.
                currentStart = block;
                currentEnd = block;
                currentHandlers = handlers;
                continue;
            }

            if (currentHandlers.equals(handlers)
                    && rangeIsValid(currentStart, block)) {
                /*
                 * The block we are looking at now has the same handlers
                 * as the block that started the currently open catch
                 * range, and adding it to the currently open range won't
                 * cause it to be too long.
                 */
                currentEnd = block;
                continue;
            }

            /*
             * The block we are looking at now has incompatible handlers,
             * so we need to finish off the last entry and start a new
             * one. Note: We only emit an entry if it has associated handlers.
             */
            if (currentHandlers.size() != 0) {
                resultList.add(makeEntry(currentStart, currentEnd,
                        currentHandlers));
            }

            currentStart = block;
            currentEnd = block;
            currentHandlers = handlers;
        }

        if (currentHandlers.size() != 0) {
            // Emit an entry for the range that was left hanging.
            resultList.add(makeEntry(currentStart, currentEnd,
                    currentHandlers));
        }

        // Construct the final result.

        int resultSz = resultList.size();

        if (resultSz == 0) {
            return CatchTable.EMPTY;
        }

        CatchTable result = new CatchTable(resultSz);

        for (int i = 0; i < resultSz; i++) {
            result.set(i, resultList.get(i));
        }

        result.setImmutable();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAnyCatches() {
        return hasAnyCatches;
    }

    /** {@inheritDoc} */
    @Override
    public HashSet<Type> getCatchTypes() {
        // Return a copy, so callers can't modify our internal state.
        return new HashSet<>(catchTypes);
    }

    /**
     * Builds and returns the catch table for a given method.
     *
     * @param method {@code non-null;} method to build the list for
     * @param order {@code non-null;} block output order
     * @param addresses {@code non-null;} address objects for each block
     * @return {@code non-null;} the constructed table
     */
    public static CatchTable build(RopMethod method, int[] order,
            BlockAddresses addresses) {
        return new StdCatchBuilder(method, order, addresses).build();
    }

    /**
     * Makes a {@link CatchTable.Entry} for the given block range and
     * handlers.
     *
     * @param start {@code non-null;} the start block for the range (inclusive)
     * @param end {@code non-null;} the end block for the range (also inclusive)
     * @param handlers {@code non-null;} the handlers for the range
     * @return {@code non-null;} an appropriately-constructed instance
     */
    private static CatchTable.Entry makeEntry(ThrowingBlock start,
            ThrowingBlock end, CatchHandlerList handlers) {
        /*
         * We start at the *last* instruction of the start block, since
         * that's the instruction that can throw...
         */
        CodeAddress startAddress = start.start;

        // ...And we end *after* the last instruction of the end block.
        CodeAddress endAddress = end.end;

        return new CatchTable.Entry(startAddress.getAddress(),
                endAddress.getAddress(), handlers);
    }

    /**
     * Gets whether the address range for the given two blocks is valid
     * for a catch handler. This is true as long as the covered range is
     * under 65536 code units.
     *
     * @param start {@code non-null;} the start block for the range (inclusive)
     * @param end {@code non-null;} the end block for the range (also inclusive)
     * @return {@code true} if the range is valid
     */
    private static boolean rangeIsValid(ThrowingBlock start,
            ThrowingBlock end) {
        // See above about selection of instructions.
        int startAddress = start.start.getAddress();
        int endAddress = end.end.getAddress();

        return (endAddress - startAddress) <= MAX_CATCH_RANGE;
    }
}
