/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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

public class ErrorCodes
{
    // Error codes - system
    public static final int ERROR_UNEXPECTED_EXCEPTION = 1000;
    public static final int ERROR_NULL = 1001;
    public static final int ERROR_ASSERTION_FAILED = 1002;
    public static final int ERROR_UNSUPPORTED = 1003;
    
    // Error codes - internal
    public static final int ERROR_CODE_ERROR = 2000;
    public static final int ERROR_CLASS_NOT_FOUND = 2001;
    public static final int ERROR_CLASS_CAST = 2002;
    
    // Error codes - config
    public static final int ERROR_FILE_NOT_FOUND = 3000;
    public static final int ERROR_JDOM_PARSE_FAILED = 3001;
    public static final int ERROR_RENAME = 3002;
    public static final int ERROR_XSD_PARSE_FAILED = 3003;
    public static final int ERROR_CREATE = 3004;
    public static final int ERROR_INVALID = 3005;
    public static final int ERROR_RESOURCES = 3006;
    
    // Error codes - validation
    public static final int ERROR_VALIDATION = 4000;
    public static final int ERROR_NOT_FOUND = 4001;
    
    // Error codes - server
    public static final int ERROR_SERVER_INVALID_MESSAGE = 5000;
    public static final int ERROR_SERVER_UNKNOWN_MESSAGE  =5001;
    public static final int ERROR_SERVER_IO = 5002;
    public static final int ERROR_SERVER_FORBIDDEN = 5003;
    public static final int ERROR_SERVER_NO_PORTS = 5004;
    
    // Error codes - messaging
    public static final int ERROR_BAD_EMAIL_ADDRESS = 6000;
    public static final int ERROR_COMPOSITION = 6001;
    public static final int ERROR_SEND_FAILED = 6002;
    public static final int ERROR_BAD_PROVIDER = 6003;
    public static final int ERROR_MESSAGING = 6004;
    public static final int ERROR_INVALID_MESSAGE = 6005;
    
    // Error codes - ddmessenger
    public static final int ERROR_BAD_RESPONSE = 7000; // add response
    public static final int ERROR_404 = 7404; // not found (jboss starting)
    public static final int ERROR_403 = 7403; // not ready (jboss starting)
    public static final int ERROR_503 = 7503; // servlet not init (jboss starting)

    // Error codes - database
    public static final int ERROR_DATABASE_UNINITIALIZED = 8000;
    public static final int ERROR_DATABASE_UNAVAILABLE = 8001;
}