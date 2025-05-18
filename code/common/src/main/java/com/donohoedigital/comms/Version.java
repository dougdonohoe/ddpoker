/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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

/**
 * @author donohoe
 */
@DataCoder('V')
public class Version implements DataMarshal
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
     * Create a new instance of Version using the string format
     */
    public Version(String s)
    {
        char c;
        int beginIndex = 0;
        int endIndex = -1;
        int length = s.length();

        while (Character.isDigit(c = s.charAt(++endIndex)))
        {
        }
        nMajor_ = Integer.parseInt(s.substring(beginIndex, endIndex));

        beginIndex = endIndex + 1;
        while ((++endIndex < length) && (Character.isDigit(c = s.charAt(endIndex))))
        {
        }
        nMinor_ = Integer.parseInt(s.substring(beginIndex, endIndex));

        if (endIndex == length) return;
        bAlpha_ = (c == 'a');
        bBeta_ = (c == 'b');
        if (bAlpha_ || bBeta_)
        {
            beginIndex = endIndex + 1;
            while ((++endIndex < length) && (Character.isDigit(c = s.charAt(endIndex))))
            {
            }
            nAlphaBetaVersion_ = Integer.parseInt(s.substring(beginIndex, endIndex));
        }

        if (endIndex == length) return;
        if (c == 'p' || c == '.') // old style is 3.1p2, new is 3.1.2
        {
            beginIndex = endIndex + 1;
            while ((++endIndex < length) && (Character.isDigit(c = s.charAt(endIndex))))
            {
            }
            nPatch_ = Integer.parseInt(s.substring(beginIndex, endIndex));
        }

        if (endIndex == length) return;
        bDemo_ = (c == 'd');
        if (bDemo_)
        {
            if (++endIndex < length) c = s.charAt(endIndex);
        }

        if (endIndex == length) return;
        if (c == '_')
        {
            beginIndex = endIndex + 1;
            sLocale_ = s.substring(beginIndex);
        }
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
     * than given version
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean isMajorMinorBefore(Version version)
    {
        if (nMajor_ > version.nMajor_) return false;
        if (nMajor_ < version.nMajor_) return true;
        if (nMinor_ > version.nMinor_) return false;
        if (nMinor_ < version.nMinor_) return true;
        if (bAlpha_ && version.bBeta_) return true;
        if (bBeta_ && version.bAlpha_) return false;
        if (!isProduction() && version.isProduction()) return true;
        if (isProduction() && !version.isProduction()) return false;
        if (bAlpha_ && version.bAlpha_ && nAlphaBetaVersion_ > version.nAlphaBetaVersion_) return false;
        if (bAlpha_ && version.bAlpha_ && nAlphaBetaVersion_ < version.nAlphaBetaVersion_) return true;
        if (bBeta_ && version.bBeta_ && nAlphaBetaVersion_ > version.nAlphaBetaVersion_) return false;
        if (bBeta_ && version.bBeta_ && nAlphaBetaVersion_ < version.nAlphaBetaVersion_) return true;

        return false;
    }

    /**
     * Return true if this version is an earlier version
     * than given version
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean isBefore(Version version)
    {
        if (nMajor_ > version.nMajor_) return false;
        if (nMajor_ < version.nMajor_) return true;
        if (nMinor_ > version.nMinor_) return false;
        if (nMinor_ < version.nMinor_) return true;
        if (bAlpha_ && version.bBeta_) return true;
        if (bBeta_ && version.bAlpha_) return false;
        if (!isProduction() && version.isProduction()) return true;
        if (isProduction() && !version.isProduction()) return false;
        if (bAlpha_ && version.bAlpha_ && nAlphaBetaVersion_ > version.nAlphaBetaVersion_) return false;
        if (bAlpha_ && version.bAlpha_ && nAlphaBetaVersion_ < version.nAlphaBetaVersion_) return true;
        if (bBeta_ && version.bBeta_ && nAlphaBetaVersion_ > version.nAlphaBetaVersion_) return false;
        if (bBeta_ && version.bBeta_ && nAlphaBetaVersion_ < version.nAlphaBetaVersion_) return true;
        if (nPatch_ > version.nPatch_) return false;
        if (nPatch_ < version.nPatch_) return true;

        return false;
    }

    /**
     * Return true if this version is a later version
     * than given version
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean isAfter(Version version)
    {
        if (nMajor_ < version.nMajor_) return false;
        if (nMajor_ > version.nMajor_) return true;
        if (nMinor_ < version.nMinor_) return false;
        if (nMinor_ > version.nMinor_) return true;
        if (bBeta_ && version.bAlpha_) return true;
        if (bAlpha_ && version.bBeta_) return false;
        if (!isProduction() && version.isProduction()) return false;
        if (isProduction() && !version.isProduction()) return true;
        if (bAlpha_ && version.bAlpha_ && nAlphaBetaVersion_ < version.nAlphaBetaVersion_) return false;
        if (bAlpha_ && version.bAlpha_ && nAlphaBetaVersion_ > version.nAlphaBetaVersion_) return true;
        if (bBeta_ && version.bBeta_ && nAlphaBetaVersion_ < version.nAlphaBetaVersion_) return false;
        if (bBeta_ && version.bBeta_ && nAlphaBetaVersion_ > version.nAlphaBetaVersion_) return true;
        if (nPatch_ < version.nPatch_) return false;
        if (nPatch_ > version.nPatch_) return true;

        return false;
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
}
