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
 * SoundTest.java
 *
 * Created on October 13, 2005, 8:55 AM 
 */

package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.udp.*;

/**
 *
 * @author  Doug Donohoe
 */
public class SoundTest extends BaseCommandLineApp
{
    // logging
    private Logger logger;

    /**
     * Run emailer
     */
    public static void main(String[] args) {
        try {
            Prefs.setRootNodeName("poker2");
            new SoundTest("poker", args);
        }

        catch (ApplicationError ae)
        {
            System.err.println("SoundTest ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }  
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
        catch (Throwable t)
        {
            System.err.println("SoundTest ending due to ApplicationError: " + Utils.formatExceptionText(t));
            System.exit(1);
        }
    }
    
    /**
     * Can be overridden for application specific options
     */
    protected void setupApplicationCommandLineOptions()
    {

    }

    UDPServer udp_;


    public SoundTest(String sConfigName, String[] args)
    {
        super(sConfigName, args);
        ConfigManager.getConfigManager().loadAudioConfig();

        // init
        logger = LogManager.getLogger(getClass());

        // test
        test();

        // exit
        //logger.debug("\n"+Utils.getAllStacktraces());
        System.exit(0);

    }

    /**
     * write cards
     */
    private void test()
    {
        for (int i = 0; i < 5; i++)
        {
            AudioConfig.playFX("preffx");
            Utils.sleepMillis(1000);
            //logger.debug(Utils.getAllStacktraces());
        }
    }
}
