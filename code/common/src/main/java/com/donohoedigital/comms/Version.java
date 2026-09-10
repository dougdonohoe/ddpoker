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
 * Version.java
 *
 * Created on June 24, 2003, 5:12 PM
 */

package com.donohoedigital.comms;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author donohoe
 */
@DataCoder('V')
public class Version implements DataMarshal, Comparable<Version>
{
    private int nMajor_;
    private int nMinor_;
    private int nPatch_;
    private boolean bBeta_;
    private boolean bAlpha_;
    private int nAlphaBetaVersion_;
    private boolean bDemo_;
    private String sLocale_;
    private boolean bVerify_; // only used on client - don't send down

    public static final int TYPE_PRODUCTION = 0;
    public static final int TYPE_ALPHA = 1;
    public static final int TYPE_BETA = 2;

    /**
     * Empty needed for demarshal
     */
    public Version()
    {
    }

    /**
     * Creates a new instance of Version
     */
    public Version(int nMajor, int nMinor, int nPatch, boolean bVerify)
    {
        this(TYPE_PRODUCTION, nMajor, nMinor, 0, nPatch, bVerify);
    }

    /**
     * Creates a new instance of Version
     */
    public Version(int nType, int nMajor, int nMinor, int nAlphaBetaVersion, int nPatchVersion, boolean bVerify)
    {
        bVerify_ = bVerify;
        nMajor_ = nMajor;
        nMinor_ = nMinor;
        nPatch_ = nPatchVersion;
        bBeta_ = nType == TYPE_BETA;
        bAlpha_ = nType == TYPE_ALPHA;
        nAlphaBetaVersion_ = nAlphaBetaVersion;
        if (bDemo_ && (bBeta_ || bAlpha_)) throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR,
                                                                      "Can't be demo and alpha/beta at same time", "fix code");
    }

    public boolean isVerify()
    {
        return bVerify_;
    }

    public int getMajor()
    {
        return nMajor_;
    }

    public String getMajorAsString()
    {
        return String.valueOf(nMajor_);
    }

    public int getMinor()
    {
        return nMinor_;
    }

    public int getPatch()
    {
        return nPatch_;
    }

    public boolean isAlpha()
    {
        return bAlpha_;
    }

    public boolean isBeta()
    {
        return bBeta_;
    }

    public boolean isProduction()
    {
        return !bAlpha_ && !bBeta_;
    }

    public int getAlphaBetaVersion()
    {
        return nAlphaBetaVersion_;
    }

    public void setDemo(boolean b)
    {
        bDemo_ = b;
    }

    public boolean isDemo()
    {
        return bDemo_;
    }

    public String getLocale()
    {
        return sLocale_;
    }

    public void setLocale(String s)
    {
        sLocale_ = s;
    }

    /**
     * Return true if this version is an earlier version
     * than given version, ignoring the patch number
     */
    public boolean isMajorMinorBefore(Version version)
    {
        return compareTo(version, false) < 0;
    }

    /**
     * Return true if this version is an earlier version
     * than given version
     */
    public boolean isBefore(Version version)
    {
        return compareTo(version) < 0;
    }

    /**
     * Return true if this version is a later version
     * than given version
     */
    public boolean isAfter(Version version)
    {
        return compareTo(version) > 0;
    }

    /**
     * True if this version is a later release than the given one.  False when they are the same
     * version, so a check for a newer build does not report the one already running, and false
     * for a null other, which is what a failed lookup hands back.
     */
    public boolean isNewerThan(Version other)
    {
        return other != null && compareTo(other) > 0;
    }

    /**
     * Newest last: major, then minor, then the release type, then the alpha/beta number, then
     * the patch.  An alpha or beta of a version comes before the version itself - 2.0a3, then
     * 2.0b1, then 2.0 - and the patch is ranked last because it applies within a release, which
     * is the order this project has actually shipped in (2.0b6.4 is Beta 6, Patch 4; see the
     * history in PokerConstants).  Neither the locale nor the demo flag plays a part; they say
     * who a build is for, not when it is from.
     */
    @Override
    public int compareTo(Version o)
    {
        return compareTo(o, true);
    }

    /**
     * See {@link #compareTo(Version)}.  When bIncludePatch is false the patch number is left out,
     * so two builds of the same release compare equal - what {@link #isMajorMinorBefore} wants.
     */
    private int compareTo(Version o, boolean bIncludePatch)
    {
        if (nMajor_ != o.nMajor_) return Integer.compare(nMajor_, o.nMajor_);
        if (nMinor_ != o.nMinor_) return Integer.compare(nMinor_, o.nMinor_);
        if (typeRank() != o.typeRank()) return Integer.compare(typeRank(), o.typeRank());
        if (nAlphaBetaVersion_ != o.nAlphaBetaVersion_)
            return Integer.compare(nAlphaBetaVersion_, o.nAlphaBetaVersion_);
        return bIncludePatch ? Integer.compare(nPatch_, o.nPatch_) : 0;
    }

    /** Alpha before beta before production - see {@link #compareTo(Version)}. */
    private int typeRank()
    {
        if (bAlpha_) return 0;
        if (bBeta_) return 1;
        return 2;
    }

    /** Consistent with {@link #compareTo(Version)}, so the locale and demo flag are left out of this too. */
    @Override
    public boolean equals(Object o)
    {
        return o instanceof Version v && compareTo(v) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(nMajor_, nMinor_, typeRank(), nAlphaBetaVersion_, nPatch_);
    }

    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        nMajor_ = list.removeIntToken();
        nMinor_ = list.removeIntToken();
        bBeta_ = list.removeBooleanToken();
        nAlphaBetaVersion_ = list.removeIntToken();
        bDemo_ = list.removeBooleanToken();
        nPatch_ = list.removeIntToken();

        // Locale (added for French, 3/3/2004)
        if (list.hasMoreTokens())
        {
            sLocale_ = list.removeStringToken();

            // alpha (added for DD Poker Alpha 2, 5/13/2005)
            if (list.hasMoreTokens())
            {
                bAlpha_ = list.removeBooleanToken();
            }
        }
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(nMajor_);
        list.addToken(nMinor_);
        list.addToken(bBeta_);
        list.addToken(nAlphaBetaVersion_);
        list.addToken(bDemo_);
        list.addToken(nPatch_);
        list.addToken(sLocale_);
        list.addToken(bAlpha_);
        return list.marshal(state);
    }

    @Override
    public String toString()
    {
        return nMajor_ + "." + nMinor_ + (bAlpha_ | bBeta_ ? (bAlpha_ ? "a" : "b") + nAlphaBetaVersion_ : "") +
               (nPatch_ > 0 ? "." + nPatch_ : "") +
               (bDemo_ ? "d" : "") +
               (sLocale_ != null ? "_" + sLocale_ : "");
    }

    /**
     * Mirrors {@link #toString}: major.minor, an optional a/b number, an optional patch, an
     * optional demo flag, an optional locale.  The old "3.1p2" way of writing a patch is still
     * understood, as are release tags written with a leading "v".
     */
    private static final Pattern PARSE =
            Pattern.compile("^v?(\\d+)\\.(\\d+)(?:([ab])(\\d+))?(?:[.p](\\d+))?(d)?(?:_(.+))?$");

    /**
     * The inverse of {@link #toString}: "3.1", "3.1.8", "2.0b6.4", "1.2d", "3.1.8_en".
     *
     * @return the parsed version, or null if the string is not one.  Never throws - callers
     * parse text they did not produce, such as a release tag read off a web page.
     */
    public static Version parse(String s)
    {
        if (s == null) return null;

        Matcher m = PARSE.matcher(s.trim());
        if (!m.matches()) return null;

        try
        {
            String sType = m.group(3);
            int nType = sType == null ? TYPE_PRODUCTION : ("a".equals(sType) ? TYPE_ALPHA : TYPE_BETA);
            int nAlphaBeta = sType == null ? 0 : Integer.parseInt(m.group(4));
            int nPatch = m.group(5) == null ? 0 : Integer.parseInt(m.group(5));

            // not the running build, so nothing to verify an activation key against
            Version v = new Version(nType, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                                    nAlphaBeta, nPatch, false);
            if (m.group(6) != null) v.setDemo(true);
            v.setLocale(m.group(7));
            return v;
        }
        catch (NumberFormatException e)
        {
            // a part too big for an int - not a version we know how to compare
            return null;
        }
    }
}
