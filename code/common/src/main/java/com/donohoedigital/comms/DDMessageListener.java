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
/*
 * DDMessageListener.java
 *
 * Created on March 18, 2003, 3:55 PM
 */

package com.donohoedigital.comms;

/**
 *
 * @author  donohoe
 */
public interface DDMessageListener
{
    // status - numbered so can be used in array lookups (i.e., SendMessageDialog)
    // set in DDMessage.setStatus()
    int STATUS_NONE = -1;
    int STATUS_CONNECT_FAILED = 0;
    int STATUS_TIMEOUT = 1;
    int STATUS_SERVER_ERROR = 2;
    int STATUS_UNKNOWN_HOST = 3;
    int STATUS_UNKNOWN_ERROR = 4;
    int STATUS_DNS_TIMEOUT = 5;
    int STATUS_APPL_ERROR = 6;
    int STATUS_DISABLED = 7;

    int STATUS_OK = 10;
    
    /**
     * Called when a message has been received
     */
    void messageReceived(DDMessage message);
    
    // steps - numbered so can be used in array lookups (i.e., SendMessageDialog) 
    int STEP_CONNECTING = 0;
    int STEP_SENDING = 1;
    int STEP_WAITING_FOR_REPLY = 2;
    int STEP_RECEIVING = 3;
    int STEP_DONE = 4;
    
    // debug steps, used for fine debugging
    int DEBUG_STEP_BEGUN_CONNECTING = 10;
    int DEBUG_STEP_SLEEP_CONNECTING = 11;
    
    /**
     * Called at various times during the connection so the UI can update the 
     * status
     */
    void updateStep(int nStep);
    void debugStep(int nStep, String sMsg);
}
