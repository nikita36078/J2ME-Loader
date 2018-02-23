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

package com.android.dex.util;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception which carries around structured context.
 */
public class ExceptionWithContext extends RuntimeException {
    /** {@code non-null;} human-oriented context of the exception */
    private StringBuffer context;

    /**
     * Augments the given exception with the given context, and return the
     * result. The result is either the given exception if it was an
     * {@link ExceptionWithContext}, or a newly-constructed exception if it
     * was not.
     *
     * @param ex {@code non-null;} the exception to augment
     * @param str {@code non-null;} context to add
     * @return {@code non-null;} an appropriate instance
     */
    public static ExceptionWithContext withContext(Throwable ex, String str) {
        ExceptionWithContext ewc;

        if (ex instanceof ExceptionWithContext) {
            ewc = (ExceptionWithContext) ex;
        } else {
            ewc = new ExceptionWithContext(ex);
        }

        ewc.addContext(str);
        return ewc;
    }

    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     */
    public ExceptionWithContext(String message) {
        this(message, null);
    }

    /**
     * Constructs an instance.
     *
     * @param cause {@code null-ok;} exception that caused this one
     */
    public ExceptionWithContext(Throwable cause) {
        this(null, cause);
    }

    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     * @param cause {@code null-ok;} exception that caused this one
     */
    public ExceptionWithContext(String message, Throwable cause) {
        super((message != null) ? message :
              (cause != null) ? cause.getMessage() : null,
              cause);

        if (cause instanceof ExceptionWithContext) {
            String ctx = ((ExceptionWithContext) cause).context.toString();
            context = new StringBuffer(ctx.length() + 200);
            context.append(ctx);
        } else {
            context = new StringBuffer(200);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        out.println(context);
    }

    /** {@inheritDoc} */
    @Override
    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        out.println(context);
    }

    /**
     * Adds a line of context to this instance.
     *
     * @param str {@code non-null;} new context
     */
    public void addContext(String str) {
        if (str == null) {
            throw new NullPointerException("str == null");
        }

        context.append(str);
        if (!str.endsWith("\n")) {
            context.append('\n');
        }
    }

    /**
     * Gets the context.
     *
     * @return {@code non-null;} the context
     */
    public String getContext() {
        return context.toString();
    }

    /**
     * Prints the message and context.
     *
     * @param out {@code non-null;} where to print to
     */
    public void printContext(PrintStream out) {
        out.println(getMessage());
        out.print(context);
    }

    /**
     * Prints the message and context.
     *
     * @param out {@code non-null;} where to print to
     */
    public void printContext(PrintWriter out) {
        out.println(getMessage());
        out.print(context);
    }
}
