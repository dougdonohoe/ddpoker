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
 * LoadSavedGame.java
 *
 * Created on February 12, 2003, 11:12 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;

/**
 *
 * @author  Doug Donohoe
 */
public class LoadSavedGame extends BasePhase 
{
    
    public static final String PARAM_GAMESTATE = "gamestate";

    public void start() {
        GameState state = (GameState) gamephase_.getObject(PARAM_GAMESTATE);
        ApplicationError.assertNotNull(state, "'gamestate' not defined in params");
        context_.createGame(state);
    }
    
    /**
     * Conveience function to create params/start this phase
     */
    public static void loadGame(GameContext context, GameState state)
    {
        TypedHashMap params = new TypedHashMap();
        params.setString(ChainPhase.PARAM_NEXT_PHASE, "LoadSavedGame");
        TypedHashMap nextParams = new TypedHashMap();
        params.setObject(ChainPhase.PARAM_NEXT_PHASE_PARAMS, nextParams);

        // store selected game state for processing by LoadSaveGame
        nextParams.setObject(LoadSavedGame.PARAM_GAMESTATE, state);

        // use initial game screen to display a message while
        // loading
        context.processPhase("InitializeGameLoad", params);
    }

    /**
     * No loading in demo
     */
    public boolean isUsedInDemo()
    {
        return TESTING(EngineConstants.TESTING_DEMO);
    }

    // Notes:
    //
    // Load sequence works as such:
    //
    // 1) game main class (subclass of GameEngine typically) calls static LoadSavedGame.loadGame()
    // 2) loadGame() invokes phase InitializeGameLoad which displays a message such as "Loading..." then
    // 3) invokes LoadSavedGame phase
    // 4) this phase (LoadSavedGame) actually reads and creates game (see start())
    // 5) loading of game in Game uses processStartPhase() which invokes the
    //    begin phase returned by the GameStateDelegate, which is assumed to be a 
    //    subclass of chain phase.  Typically the begin phase creates the game board
    //    but can be any chain phase for special cases.
    // 6) After the begin phase runs, it invokes the phase stored in the game
    //    file, using the ChainPhase PARAM_NEXT_PHASE, PARAM_NEXT_PHASE_PARAMS 
    //    invocation (similar to above)
    // 7) At that point the game is loaded and running.
}
