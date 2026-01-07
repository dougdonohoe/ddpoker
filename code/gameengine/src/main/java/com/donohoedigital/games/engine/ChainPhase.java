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
 * ChainPhase.java
 *
 * Created on November 23, 2002, 3:55 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;

/**
 *
 * @author  Doug Donohoe
 */
public abstract class ChainPhase extends BasePhase {
    
    public static final String NEXT_PHASE_NONE = "NONE";
    public static final String PARAM_NEXT_PHASE = "next-phase";
    public static final String PARAM_NEXT_PHASE_PARAMS = "next-phase-params";
    
    /**
     * Calls process(), for subclass' logic and then immediately calls nextPhase()
     */
    public void start()
    {
        process();
        nextPhase();
    }
    
    /**
     * Goto the next phase using processPhase()
     */
    protected void nextPhase()
    {
        nextPhase(false);
    }

    /**
     *  Goto the next phase using processPhaseNow()
     */
    protected void nextPhaseNow()
    {
        nextPhase(true);
    }

    /**
     * next phase processing
     */
    private void nextPhase(boolean bNow)
    {
        String sNextPhase = gamephase_.getString(PARAM_NEXT_PHASE, null);
        ApplicationError.assertNotNull(sNextPhase, "next-phase not defined in ", gamephase_.getName());
        if (sNextPhase.equals(NEXT_PHASE_NONE)) return;
        
        TypedHashMap params = (TypedHashMap) gamephase_.getObject(PARAM_NEXT_PHASE_PARAMS);
        if (bNow)
        {
            context_.processPhaseNow(sNextPhase, params);
        }
        else
        {
            context_.processPhase(sNextPhase, params);
        }
    }

    /**
     * Application logic goes here.  Upon return, the next-phase is processed
     */
    public abstract void process();
    
    /**
     * Empty class which does no processing, for use in some load situations
     */
    public static class Empty extends ChainPhase
    {
        // nothing
        public void process() {
        }
        
    }
}
