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
/*
 * BaseCommandLineApp.java
 *
 * Created on September 27, 2003, 5:08 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;

public abstract class BaseCommandLineApp
{
    protected String sAppName_ = null;
    protected String sConfigDir_ = null;
    protected TypedHashMap htOptions_;
            
    public BaseCommandLineApp(String sAppName, String[] args)
    {
        CommandLine.setUsage(getClass().getName() + " [options]");
        setupApplicationCommandLineOptions();
        
        init(sAppName, args);
    }
    
    /**
     * Can be overridden for application specific options
     */
    protected void setupApplicationCommandLineOptions()
    {
    }

    /**
     * Get command line options
     */
    public TypedHashMap getCommandLineOptions()
    {
        return htOptions_;
    }

    /**
     * Main init function to be called by subclass applications
     * from main()
     */
    protected void init(String sAppName, String[] args)
    {
        sAppName_ = sAppName;

        // get command line options
        CommandLine.parseArgs(args);
        htOptions_ = CommandLine.getOptions();

        // init config files
        new ConfigManager(sAppName, ApplicationType.COMMAND_LINE);
    }
}
