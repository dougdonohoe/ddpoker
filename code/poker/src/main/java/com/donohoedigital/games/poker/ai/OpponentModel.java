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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.engine.Hand;

public class OpponentModel
{
    int handsPlayed;

    FloatTracker tightness[] = new FloatTracker[6];
    FloatTracker aggression[] = new FloatTracker[6];

    BooleanTracker handsPaid;
    BooleanTracker handsLimped;
    BooleanTracker handsFoldedUnraised;
    BooleanTracker handsRaisedPreFlop;
    BooleanTracker handsOverbetPotPostFlop;
    BooleanTracker handsBetFoldPostFlop;

    BooleanTracker actFlop;
    BooleanTracker checkFoldFlop;
    BooleanTracker openFlop;
    BooleanTracker raiseFlop;

    BooleanTracker actTurn;
    BooleanTracker checkFoldTurn;
    BooleanTracker openTurn;
    BooleanTracker raiseTurn;

    BooleanTracker actRiver;
    BooleanTracker checkFoldRiver;
    BooleanTracker openRiver;
    BooleanTracker raiseRiver;

    boolean overbetPotPostFlop = false;

    public void init()
    {
        handsPlayed = 0;

        for (int i = 0; i < 6; ++i)
        {
            if (tightness[i] == null) tightness[i] = new FloatTracker(20, 10);
            else tightness[i].clear();
            if (aggression[i] == null) aggression[i] = new FloatTracker(20, 4);
            else aggression[i].clear();
        }

        if (handsPaid == null) handsPaid = new BooleanTracker(10, 6);
        else handsPaid.clear();

        if (handsLimped == null) handsLimped = new BooleanTracker(10, 6);
        else handsLimped.clear();

        if (handsFoldedUnraised == null) handsFoldedUnraised = new BooleanTracker(10, 6);
        else handsFoldedUnraised.clear();

        if (handsRaisedPreFlop == null) handsRaisedPreFlop = new BooleanTracker(10, 4);
        else handsRaisedPreFlop.clear();

        if (handsOverbetPotPostFlop == null) handsOverbetPotPostFlop = new BooleanTracker(10, 4);
        else handsOverbetPotPostFlop.clear();

        if (handsBetFoldPostFlop == null) handsBetFoldPostFlop = new BooleanTracker(10, 4);
        else handsBetFoldPostFlop.clear();

        if (actFlop == null) actFlop = new BooleanTracker(10, 4);
        else actFlop.clear();

        if (checkFoldFlop == null) checkFoldFlop = new BooleanTracker(10, 4);
        else checkFoldFlop.clear();

        if (openFlop == null) openFlop = new BooleanTracker(10, 4);
        else openFlop.clear();

        if (raiseFlop == null) raiseFlop = new BooleanTracker(10, 4);
        else raiseFlop.clear();

        if (actTurn == null) actTurn = new BooleanTracker(10, 4);
        else actTurn.clear();

        if (checkFoldTurn == null) checkFoldTurn = new BooleanTracker(10, 4);
        else checkFoldTurn.clear();

        if (openTurn == null) openTurn = new BooleanTracker(10, 4);
        else openTurn.clear();

        if (raiseTurn == null) raiseTurn = new BooleanTracker(10, 4);
        else raiseTurn.clear();

        if (actRiver == null) actRiver = new BooleanTracker(10, 4);
        else actRiver.clear();

        if (checkFoldRiver == null) checkFoldRiver = new BooleanTracker(10, 4);
        else checkFoldRiver.clear();

        if (openRiver == null) openRiver = new BooleanTracker(10, 4);
        else openRiver.clear();

        if (raiseRiver == null) raiseRiver = new BooleanTracker(10, 4);
        else raiseRiver.clear();
    }

    public void endHand(PokerAI ai, HoldemHand hhand, PokerPlayer player)
    {
        ++handsPlayed;

        Hand hand = player.isCardsExposed() ? player.getHand() : null;
        Hand community = hhand.getCommunity();

        PocketRanks flopRanks = PocketRanks.getInstance(community);

        Hand flop = ((community != null) && (community.size() > 2)) ?
            new Hand(community.getCard(0), community.getCard(1), community.getCard(2)) : null;

        boolean couldLimp = hhand.couldLimp(player);
        Boolean raisedPreFlop = hhand.betPot(player, HoldemHand.ROUND_PRE_FLOP);
        Boolean openedFlop = hhand.betPot(player, HoldemHand.ROUND_FLOP);
        Boolean raisedFlop = hhand.raisedPot(player, HoldemHand.ROUND_FLOP);
        Boolean openedTurn = hhand.betPot(player, HoldemHand.ROUND_TURN);
        Boolean raisedTurn = hhand.raisedPot(player, HoldemHand.ROUND_TURN);
        Boolean openedRiver = hhand.betPot(player, HoldemHand.ROUND_RIVER);
        Boolean raisedRiver = hhand.raisedPot(player, HoldemHand.ROUND_RIVER);

        handsRaisedPreFlop.addEntry(raisedPreFlop);
        openFlop.addEntry(openedFlop);
        raiseFlop.addEntry(raisedFlop);
        openTurn.addEntry(openedTurn);
        raiseTurn.addEntry(raisedTurn);
        openRiver.addEntry(openedRiver);
        raiseRiver.addEntry(raisedRiver);

        HandAction firstAction = hhand.getFirstVoluntaryAction(player, HoldemHand.ROUND_PRE_FLOP);
        int action = (firstAction != null) ? firstAction.getAction() : HandAction.ACTION_NONE;

        if (firstAction != null)
        {
            if (action != HandAction.ACTION_CHECK)
            {
                // don't count hands where player didn't get a chance to act
                handsPaid.addEntry(hhand.paidToPlay(player));
            }

            int positionIndex = getPreFlopPositionIndex(player.getStartingPositionCategory());

            switch (action)
            {
                case HandAction.ACTION_FOLD:
                    tightness[positionIndex].addEntry(1.0f);
                    tightness[0].addEntry(1.0f);
                    break;
                case HandAction.ACTION_CALL:
                    aggression[positionIndex].addEntry(0.0f);
                    aggression[0].addEntry(0.0f);
                    tightness[positionIndex].addEntry(0.0f);
                    tightness[0].addEntry(0.0f);
                    break;
                case HandAction.ACTION_RAISE:
                    aggression[positionIndex].addEntry(1.0f);
                    aggression[0].addEntry(1.0f);
                    tightness[positionIndex].addEntry(0.0f);
                    tightness[0].addEntry(0.0f);
                    break;
            }
        }

        if (couldLimp)
        {
            handsLimped.addEntry(action == HandAction.ACTION_CALL);
            handsFoldedUnraised.addEntry(action == HandAction.ACTION_FOLD);
        }

        if (hhand.getLastActionAI(player, HoldemHand.ROUND_FLOP) != HandAction.ACTION_NONE)
        {
            handsOverbetPotPostFlop.addEntry(overbetPotPostFlop);
        }

        HandAction firstFlopAction = hhand.getFirstVoluntaryAction(player, HoldemHand.ROUND_FLOP);
        HandAction lastFlopAction = hhand.getLastAction(player, HoldemHand.ROUND_FLOP);

        if (lastFlopAction != null)
        {
            switch (lastFlopAction.getAction())
            {
                case HandAction.ACTION_CHECK:
                case HandAction.ACTION_FOLD:
                    checkFoldFlop.addEntry(true);
                    break;
                default:
                    checkFoldFlop.addEntry(false);
                    break;
            }
        }

        actFlop.addEntry(firstFlopAction != null);

        if ((firstFlopAction != null) && (firstFlopAction.getAction() == HandAction.ACTION_BET))
        {
            if (lastFlopAction != null)
            {
                switch (lastFlopAction.getAction())
                {
                    case HandAction.ACTION_CALL:
                    case HandAction.ACTION_RAISE:
                        handsBetFoldPostFlop.addEntry(false);
                        break;
                    case HandAction.ACTION_FOLD:
                        handsBetFoldPostFlop.addEntry(true);
                        break;
                }
            }
        }

        HandAction firstTurnAction = hhand.getFirstVoluntaryAction(player, HoldemHand.ROUND_TURN);
        HandAction lastTurnAction = hhand.getLastAction(player, HoldemHand.ROUND_TURN);

        if (lastTurnAction != null)
        {
            switch (lastTurnAction.getAction())
            {
                case HandAction.ACTION_CHECK:
                case HandAction.ACTION_FOLD:
                    checkFoldTurn.addEntry(true);
                    break;
                default:
                    checkFoldTurn.addEntry(false);
                    break;
            }
        }

        actTurn.addEntry(firstTurnAction != null);

        if ((firstTurnAction != null) && (firstTurnAction.getAction() == HandAction.ACTION_BET))
        {
            if (lastTurnAction != null)
            {
                switch (lastTurnAction.getAction())
                {
                    case HandAction.ACTION_CALL:
                    case HandAction.ACTION_RAISE:
                        handsBetFoldPostFlop.addEntry(false);
                        break;
                    case HandAction.ACTION_FOLD:
                        handsBetFoldPostFlop.addEntry(true);
                        break;
                }
            }
        }

        HandAction firstRiverAction = hhand.getFirstVoluntaryAction(player, HoldemHand.ROUND_RIVER);
        HandAction lastRiverAction = hhand.getLastAction(player, HoldemHand.ROUND_RIVER);

        if (lastRiverAction != null)
        {
            switch (lastRiverAction.getAction())
            {
                case HandAction.ACTION_CHECK:
                case HandAction.ACTION_FOLD:
                    checkFoldRiver.addEntry(true);
                    break;
                default:
                    checkFoldRiver.addEntry(false);
                    break;
            }
        }

        actRiver.addEntry(firstRiverAction != null);

        if ((firstRiverAction != null) && (firstRiverAction.getAction() == HandAction.ACTION_BET))
        {
            if (lastRiverAction != null)
            {
                switch (lastRiverAction.getAction())
                {
                    case HandAction.ACTION_CALL:
                    case HandAction.ACTION_RAISE:
                        handsBetFoldPostFlop.addEntry(false);
                        break;
                    case HandAction.ACTION_FOLD:
                        handsBetFoldPostFlop.addEntry(true);
                        break;
                }
            }
        }

        overbetPotPostFlop = false;
    }

    public float getPreFlopTightness(int position, float defVal)
    {
        int index = getPreFlopPositionIndex(position);

        if (tightness[index].isReady())
            return tightness[index].getWeightedAverage(defVal);
        else
            return tightness[0].getWeightedAverage(defVal);
    }

    public float getPreFlopAggression(int position, float defVal)
    {
        int index = getPreFlopPositionIndex(position);

        if (aggression[index].isReady())
            return aggression[index].getWeightedAverage(defVal);
        else
            return aggression[0].getWeightedAverage(defVal);
    }

    public int getHandsPlayed()
    {
        return handsPlayed;
    }

    public float getActPostFlop(int round, float defval)
    {
        switch (round)
        {
            case HoldemHand.ROUND_FLOP:
                return actFlop.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_TURN:
                return actTurn.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_RIVER:
                return actRiver.getWeightedPercentTrue(defval);
            default:
                throw new ApplicationError("getActPostFlop for invalid round " + round);
        }
    }

    public float getOpenPostFlop(int round, float defval)
    {
        switch (round)
        {
            case HoldemHand.ROUND_FLOP:
                return openFlop.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_TURN:
                return openTurn.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_RIVER:
                return openRiver.getWeightedPercentTrue(defval);
            default:
                throw new ApplicationError("getOpenPostFlop for invalid round " + round);
        }
    }

    public float getRaisePostFlop(int round, float defval)
    {
        switch (round)
        {
            case HoldemHand.ROUND_FLOP:
                return raiseFlop.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_TURN:
                return raiseTurn.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_RIVER:
                return raiseRiver.getWeightedPercentTrue(defval);
            default:
                throw new ApplicationError("getRaisePostFlop for invalid round " + round);
        }
    }

    public float getCheckFoldPostFlop(int round, float defval)
    {
        switch (round)
        {
            case HoldemHand.ROUND_FLOP:
                return checkFoldFlop.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_TURN:
                return checkFoldTurn.getWeightedPercentTrue(defval);
            case HoldemHand.ROUND_RIVER:
                return checkFoldRiver.getWeightedPercentTrue(defval);
            default:
                throw new ApplicationError("getCheckFoldPostFlop for invalid round " + round);
        }
    }

    private int getPreFlopPositionIndex(int position)
    {
        switch (position)
        {
            case PokerAI.POSITION_EARLY:
                return 1;
            case PokerAI.POSITION_MIDDLE:
                return 2;
            case PokerAI.POSITION_LATE:
                return 3;
            case PokerAI.POSITION_SMALL:
                return 4;
            case PokerAI.POSITION_BIG:
                return 5;
            default:
                return 0;
        }
    }

    public void saveToMap(DMTypedHashMap map, String sPrefix)
    {
        map.setInteger(sPrefix + "handsPlayed", handsPlayed);
        map.setObject(sPrefix + "handsPaid", handsPaid.encode());
        map.setObject(sPrefix + "handsLimped", handsLimped.encode());
        map.setObject(sPrefix + "handsFoldedUnraised", handsFoldedUnraised.encode());
        map.setObject(sPrefix + "handsRaisedPreFlop", handsRaisedPreFlop.encode());
        map.setObject(sPrefix + "handsOverbetPotPostFlop", handsOverbetPotPostFlop.encode());
        map.setObject(sPrefix + "handsBetFoldPostFlop", handsBetFoldPostFlop.encode());
        map.setObject(sPrefix + "actFlop", actFlop.encode());
        map.setObject(sPrefix + "checkFoldFlop", checkFoldFlop.encode());
        map.setObject(sPrefix + "openFlop", openFlop.encode());
        map.setObject(sPrefix + "raiseFlop", raiseFlop.encode());
        map.setObject(sPrefix + "actTurn", actTurn.encode());
        map.setObject(sPrefix + "checkFoldTurn", checkFoldTurn.encode());
        map.setObject(sPrefix + "openTurn", openTurn.encode());
        map.setObject(sPrefix + "raiseTurn", raiseTurn.encode());
        map.setObject(sPrefix + "checkFoldRiver", checkFoldRiver.encode());
        map.setObject(sPrefix + "actRiver", actRiver.encode());
        map.setObject(sPrefix + "openRiver", openRiver.encode());
        map.setObject(sPrefix + "raiseRiver", raiseRiver.encode());
        for (int i = 0; i < 6; ++i)
        {
            map.setObject(sPrefix + "tightness" + i, tightness[i].encode());
            map.setObject(sPrefix + "aggression" + i, aggression[i].encode());
        }

        map.setBoolean("overbetPotPostFlop", overbetPotPostFlop);
    }

    public void loadFromMap(DMTypedHashMap map, String sPrefix)
    {
        handsPlayed = map.getInteger(sPrefix + "handsPlayed", 0);
        handsPaid.decode(map.getObject(sPrefix + "handsPaid"));
        handsLimped.decode(map.getObject(sPrefix + "handsLimped"));
        handsFoldedUnraised.decode(map.getObject(sPrefix + "handsFoldedUnraised"));
        handsRaisedPreFlop.decode(map.getObject(sPrefix + "handsRaisedPreFlop"));
        handsOverbetPotPostFlop.decode(map.getObject(sPrefix + "handsOverbetPotPostFlop"));
        handsBetFoldPostFlop.decode(map.getObject(sPrefix + "handsBetFoldPostFlop"));
        actFlop.decode(map.getObject(sPrefix + "actFlop"));
        checkFoldFlop.decode(map.getObject(sPrefix + "checkFoldFlop"));
        openFlop.decode(map.getObject(sPrefix + "openFlop"));
        raiseFlop.decode(map.getObject(sPrefix + "raiseFlop"));
        actTurn.decode(map.getObject(sPrefix + "actTurn"));
        checkFoldTurn.decode(map.getObject(sPrefix + "checkFoldTurn"));
        openTurn.decode(map.getObject(sPrefix + "openTurn"));
        raiseTurn.decode(map.getObject(sPrefix + "raiseTurn"));
        actRiver.decode(map.getObject(sPrefix + "actRiver"));
        checkFoldRiver.decode(map.getObject(sPrefix + "checkFoldRiver"));
        openRiver.decode(map.getObject(sPrefix + "openRiver"));
        raiseRiver.decode(map.getObject(sPrefix + "raiseRiver"));
        for (int i = 0; i < 6; ++i)
        {
            tightness[i].decode(map.getObject(sPrefix + "tightness" + i));
            aggression[i].decode(map.getObject(sPrefix + "aggression" + i));
        }

        overbetPotPostFlop = map.getBoolean("overbetPotPostFlop", false);
    }
}
