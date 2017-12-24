/*
* Copyright (c) 2003 Nokia Corporation and/or its subsidiary(-ies).
* All rights reserved.
* This component and the accompanying materials are made available
* under the terms of "Eclipse Public License v1.0"
* which accompanies this distribution, and is available
* at the URL "http://www.eclipse.org/legal/epl-v10.html".
*
* Initial Contributors:
* Nokia Corporation - initial contribution.
*
* Contributors:
*
* Description:
*
*/


package javax.microedition.m3g;

/**
 * Class that wraps runnable in order to catch and forward
 * exceptions that occured when executing in UI thread.
 * This class is used only for running native methods in UI thread.
 */
abstract class M3gRunnable implements Runnable
{
    private Throwable e = null;

    /**
     * From Runnable interface
     */
    public void run()
    {
        try
        {
            doRun();
        }
        catch (Throwable t)
        {
            e = t;
        }
    }

    /**
     * Checks for possible exceptions and errors and throws them forward.
     * Only unchecked exceptions and errors are thrown as only checked
     * exception that m3gcore may throw comes from loader which is not
     * executed in UI thread
     *
     * @throws RuntimeException
     * @throws Error
     */
    public void checkAndThrow()
    {
        if (e == null)
        {
            return;
        }
        if (e instanceof RuntimeException)
        {
            throw(RuntimeException)e;
        }
        else if (e instanceof Error)
        {
            throw(Error)e;
        }
    }

    /**
     * Method to be implemented for the UI thead execution
     */
    abstract void doRun();
}