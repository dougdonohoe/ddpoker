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
 * GamePhase.java
 *
 * Created on October 28, 2002, 4:49 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import org.jdom2.*;

/**
 *
 * @author  Doug Donohoe
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class GamePhase extends TypedHashMap
{
    static Logger logger = LogManager.getLogger(GamePhase.class);
   
    private String sClassname_;	
    private Class class_;
    private String sName_;
    private String sExtends_;
    private Boolean bCache_;
    private Boolean bHistory_;
    private Boolean bTransient_;
    private String sWindow_;

    private static GamePhases allPhases_ = null;
    static void setGamePhases(GamePhases phases)
    {
        allPhases_ = phases;
    }
    
    /**
     * Return copy
     */
    public final Object clone() {
        GamePhase result = (GamePhase)super.clone();
        result.sClassname_ = sClassname_;
        result.class_ = class_;
        result.sName_ = sName_;
        result.sExtends_ = sExtends_;
        result.bCache_ = bCache_;
        result.bHistory_ = bHistory_;
        result.bTransient_ = bTransient_;
        result.sWindow_ = sWindow_;
        return result;
    }
    
    /**
     * New GamePhase from XML element
     */
    public GamePhase(Element phase, Namespace ns, String sAttrErrorDesc)
                throws ApplicationError
    {
        sName_ = XMLConfigFileLoader.getStringAttributeValue(phase, "name", true, sAttrErrorDesc);
        sClassname_ = XMLConfigFileLoader.getStringAttributeValue(phase, "class", false, sAttrErrorDesc, null);
        sExtends_ = XMLConfigFileLoader.getStringAttributeValue(phase, "extends", false, sAttrErrorDesc, null);
        bCache_ = XMLConfigFileLoader.getBooleanAttributeValue(phase, "cache", false, sAttrErrorDesc, null);
        bHistory_ = XMLConfigFileLoader.getBooleanAttributeValue(phase, "history", false, sAttrErrorDesc, null);
        bTransient_ = XMLConfigFileLoader.getBooleanAttributeValue(phase, "transient", false, sAttrErrorDesc, null);
        sWindow_ = XMLConfigFileLoader.getStringAttributeValue(phase, "window", false, sAttrErrorDesc, null);

        if (sExtends_ != null)
        {
            GamePhase extend = allPhases_.get(sExtends_);
            if (extend == null)
            {
                String sMsg = sName_ + " extends " + sExtends_ + " but that wasn't found.";
                logger.error(sMsg);
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg,
                                "Make sure order is correct in " + GamedefConfig.GAMEDEF_CONFIG + 
                                "(" + sExtends_ + " must appear before " + sName_);
            }
            else
            {
                // copy class and params from phase we extend
                if (sClassname_ == null) sClassname_ = extend.sClassname_;
                if (bCache_ == null) bCache_ = extend.bCache_;
                if (bHistory_ == null) bHistory_ = extend.bHistory_;
                if (bTransient_ == null) bTransient_ = extend.bTransient_;
                if (sWindow_ == null) sWindow_ = extend.sWindow_;
                putAll(extend);
            }
                
        }
        
        if (bCache_ == null) bCache_ = Boolean.FALSE;
        if (bHistory_ == null) bHistory_ = Boolean.FALSE;
        if (bTransient_ == null) bTransient_ = Boolean.FALSE;

        if (sClassname_ == null)
        {
            String sMsg = "Class not defined for " + sName_;
            logger.error(sMsg);
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg, "Define a class for this phase");
        }
        
        class_ = ConfigUtils.getClass(sClassname_, false);
        
        // params and paramlist in phase
        XMLConfigFileLoader.loadParams(phase, ns, this, false, false,
                            "Phase " + sName_ + " in " + GamedefConfig.GAMEDEF_CONFIG);
    }
    
    /**
     * Return name of this phase
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Get classname used by this phase
     */
    public String getClassName()
    {
        return sClassname_;
    }
    
    /**
     * Get class used by this phase
     */
    public Class getClassObject()
    {
        return class_;
    }
    
    /**
     * Should this phase be cached?
     */
    public boolean isCached()
    {
        return bCache_;
    }
    
    /**
     * Should this phase be saved in history?
     */
    public boolean isHistory()
    {
        return bHistory_;
    }
    
    /**
     * If a phase is transient, it isn't recorded as the
     * current phase.  Default is false
     */
    public boolean isTransient()
    {
        return bTransient_;
    }

    /**
     * If a phase is a window, it is opened in a new EngineWindow.
     * Default is false.
     */
    public boolean isWindow()
    {
        return sWindow_ != null;
    }

    /**
     * Get window name
     */
    public String getWindowName()
    {
        return sWindow_;
    }

    /**
     * Common gamephase param is to have param name in form of name.phase = phase name.
     * Return button name usable by DDButton if that exists, otherwise just
     * return the button name
     */
    public String getButtonNameFromParam(String sName)
    {
        String sPhase = getString(sName + ".phase");
        if (sPhase != null)
        {
            return sName + GameButton.DELIM + sPhase;
        }

        return sName;
    }

    
    /**
     * String for debugging
     */
    public String toString()
    {
        return "Phase " + sName_ + " cached: " + isCached() + " history: " + isHistory() + " params: " + super.toString();
    }
}
