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
 * MacApplication.java
 *
 * Created on May 2, 2003, 3:08 PM
 */

package com.donohoedigital.gui.mac;

import com.donohoedigital.gui.*;
import com.donohoedigital.base.*;
import com.apple.eawt.*;

/**
 * Class to interface with Mac menu items
 *
 * @author  donohoe
 */
public class MacApplication extends Application
{
    private BaseApp app_;

    /** 
     * Creates a new instance of MacApplication 
     */
    public MacApplication()
    {
        app_ = BaseApp.getBaseApp();
        addApplicationListener(new MacListener());
        setEnabledPreferencesMenu(true);
    }
    
    private class MacListener extends ApplicationAdapter
    {
        /**
         * Call BaseApp showAbout()
         */
        public void handleAbout(ApplicationEvent applicationEvent)
        {
            if (!app_.isReady()) return;
            app_.showAbout();
            applicationEvent.setHandled(true);
        }

        /** EMPTY **/
        public void handleOpenApplication(ApplicationEvent applicationEvent) 
        { 
            //System.out.println("Open application: " + applicationEvent);
        }

        /** EMPTY **/
        public void handleReOpenApplication(ApplicationEvent applicationEvent) 
        { 
            //System.out.println("Re-open application: " + applicationEvent);
        }

        /**
         * Store mac file argument passed in
         */
        public void handleOpenFile(ApplicationEvent applicationEvent) 
        {
             //System.out.println("handleOpenFile: "+applicationEvent.getFilename());
             CommandLine.setMacFileArg(applicationEvent.getFilename());
             applicationEvent.setHandled(true);
        }

        /**
         * Call BaseApp showPrefs()
         */
        public void handlePreferences(ApplicationEvent applicationEvent)
        {
            if (!app_.isReady()) return;
            app_.showPrefs();
            applicationEvent.setHandled(true);
        }

        /** EMPTY **/
        public void handlePrintFile(ApplicationEvent applicationEvent) { }

        /**
         * Call BaseApp quit()
         */
        public void handleQuit(ApplicationEvent applicationEvent)
        {
            if (!app_.isReady())
            {
                System.exit(0);
                return;
            }
            app_.quit();
            applicationEvent.setHandled(false); // we handle quit, not MacOS
        }
    }
}
