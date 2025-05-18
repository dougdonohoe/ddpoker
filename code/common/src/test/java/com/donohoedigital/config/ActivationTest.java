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
package com.donohoedigital.config;

import junit.framework.*;
import org.apache.logging.log4j.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Dec 21, 2008
 * Time: 4:24:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActivationTest extends TestCase
{
    private static final Logger logger = LogManager.getLogger(ActivationTest.class);

    public void testGuid()
    {
        String key = Activation.createKeyFromGuid(22, "BD4206D4-72B0-EA5D-6FF7-1B50F92CCD49", null);
        logger.debug("Key: " + key + " length: " + key.length());

        for (String guid : new String[]{"foo", "BD4206D4-72B0-EA5D-6FF7-1B50F92CCD4z"})
        {
            expectFail(guid);
        }
    }

    public void testValidate()
    {
        assertTrue(Activation.validate(22, "2200-0024-9421-5725", null));
        assertFalse(Activation.validate(22, "2200-0024-9421-5726", null));
        assertFalse(Activation.validate(23, "2200-0024-9421-5725", null));
        assertFalse(Activation.validate(23, "2300-0024-9421-5725", null));

        assertTrue(Activation.validate(22, "KEY-22-BD4206D4-72B0-EA5D-6FF7-1B50F92CCD49-08-3498", null));
        assertFalse(Activation.validate(23, "KEY-22-BD4206D4-72B0-EA5D-6FF7-1B50F92CCD49-08-3498", null));
        assertFalse(Activation.validate(23, "KEY-23-BD4206D4-72B0-EA5D-6FF7-1B50F92CCD49-08-3498", null));
        assertFalse(Activation.validate(22, "KEY-22-BD4206D4-72B0-EA5D-6FF7-1B50F92CCD49-08-3491", null));

    }

    private void expectFail(String guid)
    {
        try
        {
            Activation.createKeyFromGuid(0, guid, null);
            fail("should have thrown exception");
        }
        catch (Exception e)
        {
            logger.debug("Expected exception: " + e.getMessage());
        }
    }
}
