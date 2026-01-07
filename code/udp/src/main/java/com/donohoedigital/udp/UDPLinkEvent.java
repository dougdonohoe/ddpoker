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
package com.donohoedigital.udp;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 12, 2006
 * Time: 10:16:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class UDPLinkEvent
{
    // event types enum
    public enum Type
    {
        ESTABLISHED("established"), CLOSING("closing"), CLOSED("closed"),
        RECEIVED("received"),
        TIMEOUT("timeout"), POSSIBLE_TIMEOUT("possible-timeout"), RESEND_FAILURE("resend-failure"),
        SESSION_CHANGED("session-changed"),
        MTU_TEST_FINISHED("mtu-test-finished");

        // name and constructor for name
        private final String sName;
        private Type(String sName)
        {
            this.sName = sName;
        }

        /**
         * get type name for string
         */
        public String toString()
        {
            return sName;
        }
    }

    // members
    private Type type_;
    private UDPLink link_;
    private long elapsed_;
    private UDPData data_;

    /**
     * Constructor - basic
     */
    public UDPLinkEvent(Type type, UDPLink link)
    {
        init(type, link, 0, null);
    }

    /**
     * Constructor - timeout
     */
    public UDPLinkEvent(Type type, UDPLink link, long elapsed)
    {
        init(type, link, elapsed, null);
    }

    /**
     * Constructor - message
     */
    public UDPLinkEvent(Type type, UDPLink link, UDPData data)
    {
        init(type, link, 0, data);
    }

    /**
     * init
     */
    private void init(Type type, UDPLink link, long elapsed, UDPData data)
    {
        type_ = type;
        link_ = link;
        elapsed_ = elapsed;
        data_= data;
    }

    /**
     * get type
     */
    public Type getType()
    {
        return type_;
    }

    /**
     * get link
     */
    public UDPLink getLink()
    {
        return link_;
    }

    /**
     * get elapsed (for TIMEOUT/POSSIBLE_TIMEOUT)
     */
    public long getElapsed()
    {
        return elapsed_;
    }

    /**
     * Get message (for RECEIVED)
     */
    public UDPData getData()
    {
        return data_;
    }

    /**
     * debug
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        sb.append(type_.toString());
        switch(type_)
        {
            case ESTABLISHED:
            case CLOSED:
                break;

            case TIMEOUT:
            case POSSIBLE_TIMEOUT:
                sb.append("; elapsed=");
                sb.append(elapsed_);
                break;

            case RESEND_FAILURE:
            case RECEIVED:
                sb.append("; data=");
                sb.append(data_.toStringShort());
                break;
        }
        sb.append("]");
        return sb.toString();
    }
}
