/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * 
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials) 
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 * 
 * For inquiries regarding commercial licensing of this source code or 
 * the use of names, logos, images, text, or other assets, please contact 
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.base;

import org.apache.log4j.*;

/**
 * This class is used to represent an application error condition.  It
 * contains the error description, details, and a suggested solution.
 * The purpose is to report error conditions in a consistent manner
 */

public class ApplicationError extends RuntimeException
{
    private static Logger logger = Logger.getLogger(ApplicationError.class);

    private String sMessage_ = null;
    private String sDetails_ = null;
    private String sSuggestedResolution_ = null;
    private int nErrorCode_;
    boolean bDetailsIsException_ = false;
    private Throwable exception_ = null;

    /**
     * Default - code error
     */
    public ApplicationError()
    {
        this(ErrorCodes.ERROR_CODE_ERROR);
    }

    public ApplicationError(String sMessage) {
        nErrorCode_ = ErrorCodes.ERROR_CODE_ERROR;
        sMessage_ = sMessage;
        init();
    }

    /**
     * Create an application error with a message, details and suggested resolutions
     */
    public ApplicationError(int nErrorCode, String sMessage, String sDetails, String sSuggestedResolution)
    {
        nErrorCode_ = nErrorCode;
        sMessage_ = sMessage;
        sDetails_ = sDetails;
        sSuggestedResolution_ = sSuggestedResolution;
        init();
    }

    /**
     * Create an application error where an exception is used for detailed info
     */
    public ApplicationError(int nErrorCode, String sMessage, Throwable e, String sSuggestedResolution)
    {
        exception_ = e;
        nErrorCode_ = nErrorCode;
        sMessage_ = sMessage;
        setDetails(e);
        sSuggestedResolution_ = sSuggestedResolution;
        init();
    }

    /**
     * Create an application error where all one has is an exception
     */
    public ApplicationError(String sMessage, Throwable e)
    {
        exception_ = e;
        nErrorCode_ = ErrorCodes.ERROR_UNEXPECTED_EXCEPTION;
        sMessage_ = sMessage;
        setDetails(e);
        init();
    }

    /**
     * Create an application error where all one has is an exception
     */
    public ApplicationError(Throwable e)
    {
        exception_ = e;
        nErrorCode_ = ErrorCodes.ERROR_UNEXPECTED_EXCEPTION;
        sMessage_ = "This software received an unexpected error.";
        setDetails(e);
        init();
    }


    /**
     * Create an application error with just an error code - note:
     * this is to be used for internal passing of application errors and
     * not to be used to pass errors back to the user
     */
    public ApplicationError(int nErrorCode)
    {
        nErrorCode_ = nErrorCode;
        init();
    }
    
    /**
     * Create an application error where all one has is an exception, error code
     */
    public ApplicationError(int nErrorCode, Exception e)
    {
        exception_ = e;
        nErrorCode_ = nErrorCode;
        sMessage_ = "This software received an unexpected error.";
        setDetails(e);
        init();
    }

    /**
     * Create an application error with just a description and resolution.
     * The details are determined from a stack trace of this exception itself
     */
    public ApplicationError(int nErrorCode, String sMessage, String sSuggestedResolution)
    {
        nErrorCode_ = nErrorCode;
        sMessage_ = sMessage;
        sSuggestedResolution_ = sSuggestedResolution;
        bSkipToString = true;
        setDetails(this);
        bSkipToString = false;
        init();
    }
    
    private void setDetails(Throwable e)
    {
        if (e != null) {
            sDetails_ = Utils.formatExceptionText(e);
            bDetailsIsException_ = true;
        }
    }
    
    boolean bSkipToString = false;

    private void init()
    {
        if (sMessage_ == null) sMessage_ = "";
        if (sDetails_ == null) sDetails_ = "";
    }

    public int getErrorCode()
    {
        return nErrorCode_;
    }

    public String getMessage()
    {
        return sMessage_;
    }

    public String getDetails()
    {
        return sDetails_;
    }

    public String getSuggestedResolution()
    {
        return sSuggestedResolution_;
    }

    public Throwable getException()
    {
        return exception_;
    }
    
    public String toString()
    {
        if (bSkipToString) return ""; // hack for times when stack trace of 'this' needed as details
        else
        {
            String sExtra = "";
            if (!bDetailsIsException_)
            {
                bSkipToString= true;
                sExtra = ", stacktrace: " + Utils.formatExceptionText(this);
                bSkipToString= false;
            }
            return  "Error #" + nErrorCode_ +
              ", Message: " + sMessage_ +
              ", Details: " + sDetails_ +
              (sSuggestedResolution_ == null ? "" : ", Suggested Resolution: " + sSuggestedResolution_)+
              sExtra;
        }

    }
    
    public String toStringNoStackTrace()
    {
        String sDetails = ", Details: " + sDetails_;
        if (this.bDetailsIsException_) sDetails = "";
        return "Error #" + nErrorCode_ +
              ", Message: " + sMessage_ +
              sDetails +
             (sSuggestedResolution_ == null ? "" : ", Suggested Resolution: " + sSuggestedResolution_);
              
    }

    public static void assertNotNull(Object o, String sDetails)
    {
        if (o == null) throw new ApplicationError(ErrorCodes.ERROR_NULL,
                    "ASSERTION FAILURE: Unexpected null value - " + sDetails, null);
    }

    public static void assertNotNull(Object o, String sDetails, Object oInfo)
    {
        if (o == null) throw new ApplicationError(ErrorCodes.ERROR_NULL,
                    "ASSERTION FAILURE: Unexpected null value - " + sDetails, oInfo == null ? "" : oInfo.toString(), null);
    }

    public static void assertNull(Object o, String sDetails)
    {
        if (o != null) throw new ApplicationError(ErrorCodes.ERROR_NULL,
                    "ASSERTION FAILURE: Unexpected non-null value - " + sDetails, null);
    }

    public static void assertNull(Object o, String sDetails, Object oInfo)
    {
        if (o != null) throw new ApplicationError(ErrorCodes.ERROR_NULL,
                    "ASSERTION FAILURE: Unexpected non-null value - " + sDetails, oInfo == null ? "" : oInfo.toString(), null);
    }

    public static void assertTrue(boolean b, String sDetails)
    {
        if (!b) throw new ApplicationError(ErrorCodes.ERROR_ASSERTION_FAILED,
                    "ASSERTION FAILURE: Unexpected condition - " + sDetails, null);
    }
    
    public static void assertTrue(boolean b, String sDetails, Object oInfo)
    {
        if (!b) throw new ApplicationError(ErrorCodes.ERROR_ASSERTION_FAILED,
                    "ASSERTION FAILURE: Unexpected condition - " + sDetails, oInfo == null ? "" : oInfo.toString(), null);
    }

    public static void warnNotNull(Object o, String sLog)
    {
        if (o != null)
        {
            logger.warn(sLog);
        }
    }
}