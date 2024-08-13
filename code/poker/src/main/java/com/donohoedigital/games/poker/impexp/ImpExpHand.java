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
package com.donohoedigital.games.poker.impexp;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;

import java.util.*;

public class ImpExpHand
{
    public Hand community = new Hand();
    public int profileNumber;
    public int handID;
    public int tournamentID;
    public String tournamentName;
    public String hndTable;
    public String hndNumber;
    public String gameStyle;
    public String gameType;
    public Calendar startDate = Calendar.getInstance();
    public Calendar endDate = Calendar.getInstance();
    public Calendar tournamentStartDate = Calendar.getInstance();
    public Calendar tournamentEndDate = Calendar.getInstance();
    public int ante;
    public int smallBlind;
    public int bigBlind;
    public int buttonSeat;
    public int localHumanPlayerSeat = -1;
    public PokerPlayer players[] = new PokerPlayer[PokerConstants.SEATS];
    public int betChips[] = new int[PokerConstants.SEATS];
    public int overbetChips[] = new int[PokerConstants.SEATS];
    public int winChips[] = new int[PokerConstants.SEATS];
    public int startChips[] = new int[PokerConstants.SEATS];
    public int endChips[] = new int[PokerConstants.SEATS];
    public ArrayList hist = new ArrayList();
}
