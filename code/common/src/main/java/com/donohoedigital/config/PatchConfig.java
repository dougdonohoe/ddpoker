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

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.jdom.*;

import java.net.*;
import java.text.*;
import java.util.*;

/**
 * Represents the patch configuration information.
 */
public class PatchConfig extends XMLConfigFileLoader
{
    private static final String PATCHES_CONFIG = "patches.xml";

    private static PatchConfig patchConfig = null;

    private String serverSpec_ = null;
    private Map<String, List<PatchRelease>> hmReleases_ = new HashMap<String, List<PatchRelease>>();

    /**
     * Load the configuration using the given appconfig.
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
    public PatchConfig() throws ApplicationError
    {
        ApplicationError.warnNotNull(patchConfig, "PatchConfig is already initialized");
        patchConfig = this;
        init();
    }

    /**
     * Get PatchConfig instance
     */
    public static PatchConfig getPatchConfig()
    {
        return patchConfig;
    }

    /**
     * Get an update for the given OS and version.
     *
     * @param os      OS
     * @param version version
     * @return the update release, or <code>null</code> if one if not available
     */
    public PatchRelease getUpdate(String os, Version version)
    {
        List<PatchRelease> releases = hmReleases_.get(os);

        if (releases == null)
        {
            return null;
        }

        int releaseCount = releases.size();
        PatchRelease release = null;
        PatchRelease updateRelease = null;
        Version minimum = null;

        for (int i = 0; i < releaseCount; ++i)
        {
            release = releases.get(i);
            minimum = release.getMinimum();

            // Find the most recent update for the given major version.
            // If a minimum release is specified, then the passed in version must be same or higher than min vers
            if ((release.getVersion().getMajor() == version.getMajor()) && (release.getVersion().isAfter(version)) &&
                (minimum == null || !version.isBefore(minimum)))
            {
                if (updateRelease == null)
                {
                    updateRelease = release;
                }
                else if (release.getVersion().isAfter(updateRelease.getVersion()))
                {
                    updateRelease = release;
                }
            }
        }

        return updateRelease;
    }

    /**
     * Get the release for the given OS and version.
     *
     * @param os      OS
     * @param version version
     * @return the update release, or <code>null</code> if one if not available
     */
    public PatchRelease getRelease(String os, Version version)
    {
        List<PatchRelease> releases = hmReleases_.get(os);

        if (releases == null)
        {
            return null;
        }


        int releaseCount = releases.size();
        PatchRelease release = null;
        boolean releaseFound = false;
        String versionString = version.toString();

        for (int i = 0; i < releaseCount; ++i)
        {
            release = releases.get(i);

            // Find the matching version.
            if (release.getVersion().toString().equals(versionString))
            {
                releaseFound = true;
                break;
            }
        }

        return (releaseFound ? release : null);
    }

    /**
     * Load the configuration using the given appconfig.
     */
    private void init() throws ApplicationError
    {
        // if patches file is missing, no big deal
        for (URL url : new MatchingResources("classpath*:config/patches" + '/' + PATCHES_CONFIG).getAllMatchesURL())
            if (url != null)
            {
                Document doc = loadXMLUrl(url, "patches.xsd");
                init(doc);
            }
    }

    /**
     * Initialize the configuration using the given document.
     *
     * @param doc documemt
     */
    private void init(Document doc)
    {
        Element root = doc.getRootElement();

        // Get base server URL.
        serverSpec_ = getChildStringValueTrimmed(root, "url", ns_, true, PATCHES_CONFIG);

        int length = serverSpec_.length();

        if (serverSpec_.charAt(length - 1) == '/')
        {
            serverSpec_ = serverSpec_.substring(0, length - 1);
        }

        // Get releases for each OS.
        List<Element> releaseElems = getChildren(root, "release", ns_, false, PATCHES_CONFIG);
        List<Element> osElems = null;
        int releaseCount = (releaseElems != null) ? releaseElems.size() : 0;
        int osCount = 0;
        Element releaseElem = null;
        Element osElem = null;
        String desc = null;
        String osName = null;
        String dependency = null;
        String minimum = null;
        Version version = null;
        List<PatchRelease> releases = null;
        PatchRelease release = null;

        for (int i = 0; i < releaseCount; ++i)
        {
            releaseElem = releaseElems.get(i);
            version = new Version(getStringAttributeValue(releaseElem, "version", true, null));
            desc = getDescription(version);

            osElems = getChildren(releaseElem, "os", ns_, true, PATCHES_CONFIG);
            osCount = osElems.size();

            for (int j = 0; j < osCount; ++j)
            {
                osElem = osElems.get(j);
                osName = getStringAttributeValue(osElem, "name", true, null);

                releases = hmReleases_.get(osName);

                if (releases == null)
                {
                    releases = new ArrayList<PatchRelease>();
                    hmReleases_.put(osName, releases);
                }

                release = new PatchRelease(version);
                if (desc != null) release.setDescription(desc);
                release.setURL(formatURL(getChildStringValueTrimmed(osElem, "file", ns_, true, PATCHES_CONFIG)));
                release.setHash(getChildStringValueTrimmed(osElem, "hash", ns_, true, PATCHES_CONFIG));
                release.setSize(getChildIntegerValue(osElem, "size", ns_, true, PATCHES_CONFIG));

                dependency = getChildStringValueTrimmed(osElem, "dependency", ns_, false, PATCHES_CONFIG);
                if (dependency != null)
                {
                    release.setDependency(new Version(dependency));
                }

                minimum = getChildStringValueTrimmed(osElem, "minimum", ns_, false, PATCHES_CONFIG);
                if (minimum != null)
                {
                    release.setMinimum(new Version(minimum));
                }

                releases.add(release);
            }
        }
    }

    /**
     * Format a URL to the given path.
     *
     * @param path path
     * @return the formatted url
     */
    private URL formatURL(String path)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(serverSpec_);

        if (path.charAt(0) != '/')
        {
            buffer.append('/');
        }

        buffer.append(path);

        try
        {
            return new URL(buffer.toString());
        }
        catch (MalformedURLException e)
        {
            throw new ApplicationError(e);
        }
    }

    /**
     * Get the description for the given patch from an external file.
     */
    private String getDescription(Version version)
    {
        String name = version.toString() + ".html";
        String desc = null;

        URL url = new MatchingResources("classpath*:config/patches" + '/' + name).getSingleResourceURL();
        if (url != null)
        {
            desc = MessageFormat.format(ConfigUtils.readURL(url), version);
        }
        else
        {
            logger.warn("Patch description file missing: " + name);
        }

        return desc;
    }
}
