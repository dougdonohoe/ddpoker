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
package com.donohoedigital.config;

import com.donohoedigital.comms.*;

import java.net.*;

/**
 * Represents patch release information.
 */
@DataCoder('p')
public class PatchRelease implements DataMarshal
{
    private Version version_ = null;
    private URL url_ = null;
    private String hash_ = null;
    private Integer size_ = null;
    private String desc_ = null;
    private Version dependency_ = null;

    // server side only (added 2.5p1)
    private Version minimum_ = null;

    public PatchRelease()
    {
    }

    public PatchRelease(Version version)
    {
        version_ = version;
    }

    public PatchRelease(Version version, URL url, String hash, Integer size)
    {
        version_ = version;
        url_ = url;
        hash_ = hash;
        size_ = size;
    }

    public Version getVersion()
    {
        return version_;
    }

    public void setVersion(Version version)
    {
        version_ = version;
    }

    public URL getURL()
    {
        return url_;
    }

    public void setURL(URL url)
    {
        url_ = url;
    }

    public String getHash()
    {
        return hash_;
    }

    public void setHash(String hash)
    {
        hash_ = hash;
    }

    public Integer getSize()
    {
        return size_;
    }

    public void setSize(Integer size)
    {
        size_ = size;
    }

    public String getDescription()
    {
        return desc_;
    }

    public void setDescription(String desc)
    {
        desc_ = desc;
    }

    public Version getDependency()
    {
        return dependency_;
    }

    public void setDependency(Version version)
    {
        dependency_ = version;
    }

    public Version getMinimum()
    {
        return minimum_;
    }

    public void setMinimum(Version version)
    {
        minimum_ = version;
    }

    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        version_ = (Version) list.removeToken();
        try { url_ = new URL(list.removeStringToken()); } catch (MalformedURLException e) { }
        hash_ = list.removeStringToken();
        size_ = list.removeIntegerToken();
        desc_ = list.removeStringToken();
        dependency_ = (Version) list.removeToken();
        // minimum doesn't need to go to client
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(version_);
        list.addToken(url_.toString());
        list.addToken(hash_);
        list.addToken(size_);
        list.addToken(desc_);
        list.addToken(dependency_);
        // minimum not sent up from server
        return list.marshal(state);
    }
}
