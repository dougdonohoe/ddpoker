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
package com.donohoedigital.games.server.model;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.comms.Version;
import com.donohoedigital.db.model.*;
import org.apache.commons.lang.builder.*;

import javax.persistence.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 17, 2008
 * Time: 2:08:09 PM
 */
@Entity
@Table(name = "registration")
public class Registration implements BaseModel<Long>
{
    public enum Type
    {
        // unknown defined so ordinal values match legacy values
        UNKNOWN, REGISTRATION, ACTIVATION, PATCH
    }

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reg_id", nullable = false)
    private Long id;

    @Column(name = "reg_license_key", nullable = false)
    private String licenseKey;

    @Column(name = "reg_product_version", nullable = false)
    private String versionAsString;
    transient private Version version;

    @Column(name = "reg_ip_address", nullable = false)
    private String ip;

    @Column(name = "reg_host_name", nullable = true)
    private String hostName;

    @Column(name = "reg_host_name_modified", nullable = true)
    private String hostNameModified;

    @Column(name = "reg_port", nullable = true)
    private Integer port;

    @Column(name = "reg_server_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date serverTime;

    @Column(name = "reg_java_version", nullable = true)
    private String javaVersion;

    @Column(name = "reg_os", nullable = true)
    private String operatingSystem;

    @Column(name = "reg_type", nullable = false)
    @Enumerated
    private Type type;

    @Column(name = "reg_is_duplicate", nullable = false)
    private boolean duplicate;

    @Column(name = "reg_is_ban_attempt", nullable = false)
    private boolean banAttempt;

    @Column(name = "reg_name", nullable = true)
    private String name;

    @Column(name = "reg_email", nullable = true)
    private String email;

    @Column(name = "reg_address", nullable = true)
    private String address;

    @Column(name = "reg_city", nullable = true)
    private String city;

    @Column(name = "reg_state", nullable = true)
    private String state;

    @Column(name = "reg_postal", nullable = true)
    private String postal;

    @Column(name = "reg_country", nullable = true)
    private String country;

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public boolean isBanAttempt()
    {
        return banAttempt;
    }

    public void setBanAttempt(boolean banAttempt)
    {
        this.banAttempt = banAttempt;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public boolean isDuplicate()
    {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate)
    {
        this.duplicate = duplicate;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    public String getHostNameModified()
    {
        return hostNameModified;
    }

    public void setHostNameModified(String hostNameModified)
    {
        this.hostNameModified = hostNameModified;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getJavaVersion()
    {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion)
    {
        this.javaVersion = javaVersion;
    }

    public String getLicenseKey()
    {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey)
    {
        this.licenseKey = licenseKey;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getOperatingSystem()
    {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem)
    {
        this.operatingSystem = operatingSystem;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public String getPostal()
    {
        return postal;
    }

    public void setPostal(String postal)
    {
        this.postal = postal;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public Date getServerTime()
    {
        return serverTime;
    }

    public void setServerTime(Date serverTime)
    {
        this.serverTime = serverTime;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    transient private boolean ignoreSet = false;

    /**
     * version used by jpa
     */
    String getVersionAsString()
    {
        return versionAsString;
    }

    /**
     * Demarshal.  Calls setVersion (unless called from there)
     */
    synchronized void setVersionAsString(String s)
    {
        this.versionAsString = s;
        if (!ignoreSet)
        {
            ignoreSet = true;
            setVersion(s == null ? null : (Version) DataMarshaller.demarshal(s));
            ignoreSet = false;
        }
    }

    /**
     * Return version
     */
    public Version getVersion()
    {
        return version;
    }

    /**
     * Set version.  Calls setVersionAsString (unless called from there)
     * @param v
     */
    public synchronized void setVersion(Version v)
    {
        this.version = v;
        if (!ignoreSet)
        {
            ignoreSet = true;
            setVersionAsString(v == null ? null : DataMarshaller.marshal(v));
            ignoreSet = false;
        }
    }

    /**
     * On load force demarshal since JPA sets field directly
     */
    @PostLoad
    private void onLoad()
    {
        setVersionAsString(getVersionAsString());
    }

    ////
    //// Helpers
    ////

    /**
     * Is this a regular registration?
     */
    public boolean isRegistration()
    {
        return getType() == Type.REGISTRATION;
    }

    /**
     * Is this an activation?
     */
    public boolean isActivation()
    {
        return getType() == Type.ACTIVATION;
    }

    /**
     * Is this a patch activation (v1.x only)
     */
    public boolean isPatch()
    {
        return getType() == Type.PATCH;
    }

    /**
     * Is this linux?
     */
    public boolean isLinux()
    {
        return getOperatingSystem().contains("Linux");
    }

    /**
     * Is this mac?
     */
    public boolean isMac()
    {
        return getOperatingSystem().contains("Mac OS");
    }

    /**
     * Is this win?
     */
    public boolean isWin()
    {
        return getOperatingSystem().contains("Windows");
    }

    /**
     * Server time in millis
     */
    public long getServerTimeMillis()
    {
        return getServerTime().getTime();
    }

    /**
     * String representation
     */
    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE, false);
    }

    /**
     * Modify given hostname, replace numbers with #.  Used so that IPs from ISPs
     * that vary for a user end up equal.
     */
    public static String generifyHostName(String host)
    {
        if (host == null) return null;
        host = Utils.replace(host,"[0-9]+", "#");

        // special cases
        if (host.contains("mindspring.com"))
        {
            host = Utils.replace(host, "user-.*\\.ca", "user-#.ca"); // user-xxx.cable
            host = Utils.replace(host, "user-.*\\.d", "user-#.d"); // user-xxx.dialup, user-xxx.dsl
        }

        return host;
    }
}
