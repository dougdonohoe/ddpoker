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
import java.text.*;

public class ImpExpUB implements ImpExp
{
    public void setPlayerName(String name)
    {

    }
    
    public String exportTournament(ImpExpHand ieHand)
    {
        return "";
    }

    public String exportHand(ImpExpHand ieHand)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yy HH:mm:ss", Locale.US);

        String newline = "\r\n";

        StringBuilder buf = new StringBuilder();

        buf.append("----------------------------------------------------------------");
        buf.append(newline);

        buf.append(newline);

        buf.append("Hand #");
        buf.append(Long.toString(ieHand.startDate.getTime().getTime()).substring(2, 9));
        buf.append("-");
        buf.append(ieHand.hndNumber);
        buf.append(" at ");
        buf.append(ieHand.tournamentName);
        buf.append("-");
        buf.append(ieHand.hndTable);
        if ("NOLIMIT".equals(ieHand.gameStyle))
        {
            buf.append(" (No Limit tournament Hold'em)");
        }
        else if ("POTLIMIT".equals(ieHand.gameStyle))
        {
            buf.append(" (Pot Limit tournament Hold'em)");
        }
        else if ("LIMIT".equals(ieHand.gameStyle))
        {
            buf.append(" (Limit tournament Hold'em)");
        }
        buf.append(newline);

        buf.append("DDPoker is pretending to be Powered by UltimateBet");
        buf.append(newline);


        buf.append("Started at ");
        buf.append(dateFormat.format(ieHand.startDate));
        buf.append(newline);

        buf.append(newline);

        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            PokerPlayer player = ieHand.players[seat];

            if (player == null) continue;

            buf.append("     ");
            buf.append(player.getName());
            buf.append(" is at seat ");
            buf.append(seat);
            buf.append(" with ");
            buf.append(ieHand.startChips[seat]);
            buf.append(".");
            buf.append(newline);
        }

        buf.append("     ");
        buf.append("The button is at seat ");
        buf.append(ieHand.buttonSeat);
        buf.append(".");
        buf.append(newline);

        buf.append(newline);

        StringBuilder preflop = new StringBuilder();
        StringBuilder flop = new StringBuilder();
        StringBuilder turn = new StringBuilder();
        StringBuilder river = new StringBuilder();

        for (int i = 0; i < ieHand.hist.size(); ++i)
        {
            HandAction action = (HandAction)ieHand.hist.get(i);

            StringBuilder active = null;

            switch (action.getRound())
            {
                case HoldemHand.ROUND_PRE_FLOP:
                    active = preflop;
                    break;
                case HoldemHand.ROUND_FLOP:
                    active = flop;
                    break;
                case HoldemHand.ROUND_TURN:
                    active = turn;
                    break;
                case HoldemHand.ROUND_RIVER:
                    active = river;
                    break;
            }

            switch (action.getAction())
            {
                case HandAction.ACTION_ANTE:
                    buf.append("     ");
                    buf.append(action.getPlayer().getName());
                    buf.append(" posts ante (");
                    buf.append(action.getAmount());
                    buf.append(").");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_BLIND_SM:
                    buf.append("     ");
                    buf.append(action.getPlayer().getName());
                    buf.append(" posts the small blind of ");
                    buf.append(action.getAmount());
                    buf.append(".");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_BLIND_BIG:
                    buf.append("     ");
                    buf.append(action.getPlayer().getName());
                    buf.append(" posts the big blind of ");
                    buf.append(action.getAmount());
                    buf.append(".");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_FOLD:
                    if (active.length() > 0) active.append(" ");
                    active.append(action.getPlayer().getName());
                    active.append(" folds.");
                    break;
                case HandAction.ACTION_CHECK:
                case HandAction.ACTION_CHECK_RAISE:
                    if (active.length() > 0) active.append(" ");
                    active.append(action.getPlayer().getName());
                    active.append(" checks.");
                    break;
                case HandAction.ACTION_CALL:
                    if (active.length() > 0) active.append(" ");
                    active.append(action.getPlayer().getName());
                    if (action.isAllIn())
                    {
                        active.append(" goes all-in for ");
                        active.append(action.getAmount());
                        active.append(".");
                    }
                    else
                    {
                        active.append(" calls.");
                    }
                    break;
                case HandAction.ACTION_BET:
                    if (active.length() > 0) active.append(" ");
                    active.append(action.getPlayer().getName());
                    if (action.isAllIn())
                    {
                        active.append(" goes all-in for ");
                    }
                    else
                    {
                        active.append(" bets ");
                    }
                    active.append(action.getAmount());
                    active.append(".");
                    break;
                case HandAction.ACTION_RAISE:
                    if (active.length() > 0) active.append(" ");
                    active.append(action.getPlayer().getName());
                    if (action.isAllIn())
                    {
                        active.append(" goes all-in for ");
                    }
                    else
                    {
                        active.append(" bets ");
                    }
                    active.append(action.getAmount());
                    active.append(".");
                    break;
            }
        }

        buf.append(newline);

        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            PokerPlayer player = ieHand.players[seat];

            if (player == null) continue;

            buf.append("     ");
            buf.append(ieHand.players[seat].getName());
            buf.append(":  ");
            if (ieHand.players[seat].isCardsExposed())
            {
                buf.append(ieHand.players[seat].getHand().getCard(0));
                buf.append(" ");
                buf.append(ieHand.players[seat].getHand().getCard(1));
            }
            else
            {
                buf.append("-- --");
            }
            buf.append(newline);
        }

        buf.append(newline);

        buf.append("Pre-flop:");
        buf.append(newline);

        buf.append(newline);

        buf.append("          ");
        buf.append(wordWrap(preflop, newline + "          ", 66));
        buf.append(newline);

        buf.append(newline);

        if (ieHand.community.size() > 0)
        {
            if (flop.length() == 0)
            {
                appendAllInShowdown(ieHand, buf, newline);
            }

            buf.append("Flop (board: ");
            buf.append(ieHand.community.getCard(0));
            buf.append(" ");
            buf.append(ieHand.community.getCard(1));
            buf.append(" ");
            buf.append(ieHand.community.getCard(2));
            buf.append("):");
            buf.append(newline);

            buf.append(newline);

            if (flop.length() > 0)
            {
                buf.append("          ");
                buf.append(wordWrap(flop, newline + "          ", 66));
            }
            else
            {
                buf.append("        (no action in this round)");
            }
            buf.append(newline);

            buf.append(newline);

            if (ieHand.community.size() > 3)
            {
                if ((flop.length() > 0) && (turn.length() == 0))
                {
                    appendAllInShowdown(ieHand, buf, newline);
                }

                buf.append("Turn (board: ");
                buf.append(ieHand.community.getCard(0));
                buf.append(" ");
                buf.append(ieHand.community.getCard(1));
                buf.append(" ");
                buf.append(ieHand.community.getCard(2));
                buf.append(" ");
                buf.append(ieHand.community.getCard(3));
                buf.append("):");
                buf.append(newline);

                buf.append(newline);

                if (turn.length() > 0)
                {
                    buf.append("          ");
                    buf.append(wordWrap(turn, newline + "          ", 66));
                }
                else
                {
                    buf.append("        (no action in this round)");
                }
                buf.append(newline);

                buf.append(newline);

                if (ieHand.community.size() > 4)
                {
                    if (((flop.length() > 0) || (turn.length() > 0)) && (turn.length() == 0))
                    {
                        appendAllInShowdown(ieHand, buf, newline);
                    }

                    buf.append("River (board: ");
                    buf.append(ieHand.community.getCard(0));
                    buf.append(" ");
                    buf.append(ieHand.community.getCard(1));
                    buf.append(" ");
                    buf.append(ieHand.community.getCard(2));
                    buf.append(" ");
                    buf.append(ieHand.community.getCard(3));
                    buf.append(" ");
                    buf.append(ieHand.community.getCard(4));
                    buf.append("):");
                    buf.append(newline);

                    buf.append(newline);

                    if (river.length() > 0)
                    {
                        buf.append("          ");
                        buf.append(wordWrap(river, newline + "          ", 66));
                    }
                    else
                    {
                        buf.append("        (no action in this round)");
                    }
                    buf.append(newline);
                }
            }
        }

        /*
        buf.append("Hand #");
        buf.append(Long.toString(ieHand.startDate.getTime()).substring(2, 9));
        buf.append("-");
        buf.append(ieHand.hndNumber);
        buf.append(" Summary:");
        buf.append(newline);

        buf.append(newline);

        buf.append("  No rake is taken for this hand.");
        buf.append(newline);
        */
/*
          woody13 folds.   jcman folds.   ColbySophie folds.
          kdsinbham folds.   eckohockey12 folds.   RNemetz folds.
            beachrat1946 folds.   AspenX is returned 800
          (uncalled).



Hand #7619556-103 Summary:

     No rake is taken for this hand.
     AspenX wins 2800.
----------------------------------------------------------------


     mikemicdee posts the small blind of $2.
     Legendary posts the big blind of $4.

     slambamiel:  -- --
     junkie pride:  -- --
     bluffster11:  -- --
     internation:  -- --
     cmw:  -- --
     pikervinsang:  -- --
     xJMPx:  -- --
     jakeum1970:  -- --
     mikemicdee:  -- --
     Legendary:  -- --

Pre-flop:

          slambamiel calls.   junkie pride folds.   bluffster11
          raises to $18.   internation folds.   cmw folds.
          pikervinsang folds.   xJMPx folds.   jakeum1970 folds.
           mikemicdee folds.   Legendary folds.   slambamiel
          calls.

Flop (board: Js 3c Ac):

          slambamiel checks.   bluffster11 bets $42.   slambamiel
          raises to $84.   bluffster11 goes all-in for $72.
          slambamiel is returned $12 (uncalled).

Turn (board: Js 3c Ac Qs):

        (no action in this round)


River (board: Js 3c Ac Qs 5h):

        (no action in this round)




Showdown:

     slambamiel shows As Jh.
     slambamiel has As Jh Js Ac Qs: two pair, aces and jacks.
     bluffster11 shows 5s 5d.
     bluffster11 has 5s 5d Ac Qs 5h: three fives.


Hand #3667206-11279 Summary:

     $3 is raked from a pot of $186.
     bluffster11 wins $183 with three fives.
*/
        buf.append("----------------------------------------------------------------\r\n");

        return buf.toString();
    }

    private StringBuilder wordWrap(StringBuilder buf, String newline, int columns)
    {
        int index = 0;

        while (buf.length() >= index + columns)
        {
            index = buf.lastIndexOf(" ", index + columns - 1);
            buf.replace(index, index+1, newline);
            ++index;
        }

        return buf;
    }

    private void appendAllInShowdown(ImpExpHand ieHand, StringBuilder buf, String newline)
    {
        buf.append("          ");
        buf.append("Tournament all-in showdown -- players show:");
        buf.append(newline);

        buf.append(newline);

        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            PokerPlayer player = ieHand.players[seat];

            if (player == null) continue;

            if (player.isFolded()) continue;

            buf.append("               ");
            buf.append(player.getName());
            buf.append(" shows ");
            buf.append(player.getHand().getCard(0));
            buf.append(" ");
            buf.append(player.getHand().getCard(0));
            buf.append(".");
            buf.append(newline);
        }
        buf.append(newline);
    }
}
