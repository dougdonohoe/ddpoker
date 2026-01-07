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
package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import java.io.*;
import java.lang.reflect.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 5, 2008
 * Time: 8:22:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class GameStateFactory
{
    /**
     * Creates a new instance of GameState
     * from an existing file
     */
    public static GameState createGameState(File f, boolean bLoadHeader)
    {
        try
        {
            Constructor<? extends GameState> c = getClazz().getConstructor(File.class, Boolean.TYPE);
            return c.newInstance(f, bLoadHeader);
        }
        catch (Throwable t)
        {
            throw new ApplicationError(t);
        }
    }

    /**
     * Creates a new instance of GameState
     * from a byte array
     */
    public static GameState createGameState(byte[] data)
    {
        try
        {
            Constructor<? extends GameState> c = getClazz().getConstructor(byte[].class);
            //noinspection PrimitiveArrayArgumentToVariableArgMethod
            return c.newInstance(data);
        }
        catch (Throwable t)
        {
            throw new ApplicationError(t);
        }
    }

    /**
     * Create a new instance of GameState, given a name (stored in the file)
     * and an extension for the actual file.  File saved as
     * <sBegin>.NNNNNN.sExt where NNNNNN is the next available file number.
     * sDesc is a description of the game
     */
    public static GameState createGameState(String sName, String sBegin, String sExt, String sDesc)
    {
        try
        {
            Constructor<? extends GameState> c = getClazz().getConstructor(String.class, String.class, String.class, String.class);
            return c.newInstance(sName, sBegin, sExt, sDesc);
        }
        catch (Throwable t)
        {
            throw new ApplicationError(t);
        }
    }

    /**
     * Create a new instance of GameState, given a name (stored in the file)
     * and an extension for the actual file.  File saved as
     * <sBegin>.NNNNNN.sExt where NNNNNN is the next available file number.
     * sDesc is a description of the game
     */
    public static GameState createGameState(String sName, File fDir, String sBegin, String sExt, String sDesc)
    {
        try
        {
            Constructor<? extends GameState> c = getClazz().getConstructor(String.class, File.class, String.class, String.class, String.class);
            return c.newInstance(sName, fDir, sBegin, sExt, sDesc);
        }
        catch (Throwable t)
        {
            throw new ApplicationError(t);
        }
    }

    /**
     * Create game with just name and description - for use for temporary
     * game states like in online play.
     */
    public static GameState createGameState(String sName, String sDesc)
    {
        try
        {
            Constructor<? extends GameState> c = getClazz().getConstructor(String.class, String.class);
            return c.newInstance(sName, sDesc);
        }
        catch (Throwable t)
        {
            throw new ApplicationError(t);
        }
    }

    private static Class<? extends GameState> CLAZZ = null;
    /**
     * get class
     */
    @SuppressWarnings({"unchecked"})
    private synchronized static Class<? extends GameState> getClazz()
    {
        if (CLAZZ == null)
        {
            String stateClass = PropertyConfig.getRequiredStringProperty("settings.save.gamestate.class");
            CLAZZ = (Class<? extends GameState>) ConfigUtils.getClass(stateClass, true);
        }
        return CLAZZ;
    }
}
