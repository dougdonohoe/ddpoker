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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.config.*;

/**
 * Represents a range of bet size, either in terms of the pot, a player's stack, or the big blind amount.
 */
public class BetRange
{
    public static final int POT_SIZE = 1;
    public static final int STACK_SIZE = 2;
    public static final int BIG_BLIND = 3;
    public static final int ALL_IN = 4;

    private int type_;
    private float min_;
    private float max_;
    private PokerPlayer player_; // for stack-relative bets

    /**
     * Constructor for all-in bets only.
     */
    public BetRange(int type)
    {
        this (ALL_IN, null, 0.0f, 0.0f);
    }

    /**
     * Constructor for pot-relative or big-blind relative bets.
     */
    public BetRange(int type, float min, float max)
    {
        this (type, null, min, max);
    }

    /**
     * Constructor for stack-relative bets.  Pass in player whose stack is the driver.
     */
    public BetRange(PokerPlayer player, float min, float max)
    {
        this (STACK_SIZE, player, min, max);
    }

    private BetRange(int type, PokerPlayer player, float min, float max)
    {
        type_ = type;
        player_ = player;
        min_ = min;
        max_ = max;

        switch (type_)
        {
            case STACK_SIZE:
                if (player_ == null) throw new ApplicationError("Stack relative BetRange but no player.");
                break;
            case POT_SIZE:
            case BIG_BLIND:
                if (min_ == max_ && min_ == 0.0d) throw new ApplicationError("BetRange min/max both zero.");
                break;
            case ALL_IN:
                break;
            default:
                throw new ApplicationError("Unrecognized BetRange type" + type_ + '.');
        }

        if (min_ > max_)
        {
            throw new ApplicationError("BetRange min greater than max.");
        }
    }

    public int getType()
    {
        return type_;
    }

    public float getMin()
    {
        return min_;
    }

    /**
     * Get the maximum bet this range will return.  Pass in the player who is betting.
     */
    public int getMinBet(PokerPlayer player)
    {
        return chooseBetAmount(player, 0.0f);
    }

    public float getMax()
    {
        return max_;
    }

    /**
     * Get the maximum bet this range will return.  Pass in the player who is betting.
     */
    public int getMaxBet(PokerPlayer player)
    {
        return chooseBetAmount(player, 1.0f);
    }

    public PokerPlayer getPlayer()
    {
        return player_;
    }

    /**
     * Choose a random bet amount within the range.  Pass in the player who is betting.
     */
    public int chooseBetAmount(PokerPlayer player)
    {
        return chooseBetAmount(player, (float)Math.random());
    }

    private int chooseBetAmount(PokerPlayer player, float v)
    {
        if (type_ == ALL_IN)
        {
            return player.getChipCount();
        }

        HoldemHand hhand = player.getHoldemHand();

        int toCall = hhand.getCall(player);

        int betSize = 0;

        switch (type_)
        {
            case POT_SIZE:
                betSize = (int)(((hhand.getTotalPotChipCount() + toCall) * (min_ + v * (max_ - min_))));
                break;
            case STACK_SIZE:
                betSize = (int)((player_.getChipCount() - toCall) * (min_ + v * (max_ - min_)));
                break;
            case BIG_BLIND:
                betSize = (int)(hhand.getBigBlind() * (min_ + v * (max_ - min_)));
                break;
        }

        int minChip = hhand.getMinChip();

        int oddChips = betSize % minChip;

        // round up to next whole chip
        if (oddChips > 0)
        {
            betSize += minChip - oddChips;
        }

        int minBet = hhand.getMinRaise();

        // round up to minimum bet, unless min is zero, in which case round down to zero
        if (betSize < minBet)
        {
            if (min_ > 0.0d)
            {
                betSize = minBet;
            }
            else
            {
                betSize = 0;
            }
        }

        int stackSize = player.getChipCount();

        // round down to all-in bet
        if (betSize > stackSize-toCall)
        {
            betSize = stackSize-toCall;
        }

        return betSize;
    }

    public String toString(PokerPlayer player, boolean bBreak)
    {
        HoldemHand hhand = player.getHoldemHand();

        int toCall = hhand.getCall(player);
        int allIn = player.getChipCount() - toCall;

        StringBuilder buf = new StringBuilder();

        int minBet = getMinBet(player);

        if (minBet == allIn)
        {
            buf.append("All In (");
            buf.append(PropertyConfig.getAmount(minBet, true, true));
            buf.append(" chips)");
            return buf.toString();
        }

        int maxBet = getMaxBet(player);

        switch (type_)
        {
            case POT_SIZE:
            case STACK_SIZE:
                if (min_ == max_)
                {
                    buf.append((int)(min_ * 100.0d));
                    buf.append("% ");
                }
                else
                {
                    buf.append((int)(min_ * 100.0d));
                    buf.append("%-");
                    buf.append((int)(max_ * 100.0d));
                    buf.append('%');
                }
                if (type_ == POT_SIZE)
                {
                    buf.append(" pot size");
                }
                else
                {
                    if (player != player_)
                    {
                        buf.append(player_.getName());
                        buf.append("'s");
                    }
                    buf.append(" stack");
                }
                break;
            case BIG_BLIND:
                if (min_ == max_)
                {
                    buf.append(min_);
                    buf.append("x BB");
                }
                else
                {
                    buf.append(min_);
                    buf.append('-');
                    buf.append(max_);
                    buf.append("x BB");
                }
                break;
        }

        if (bBreak){
            buf.append("<br>");
        }
        else
        {
            buf.append(", or ");
        }
        buf.append('$');
        if (minBet == maxBet)
        {
            buf.append(PropertyConfig.getAmount(minBet, true, true));
        }
        else
        {
            buf.append(PropertyConfig.getAmount(minBet, true, true));
            buf.append("-$");
            buf.append(PropertyConfig.getAmount(maxBet, true, true));
            if (maxBet == allIn)
            {
                buf.append(" (All In)");
            }
        }

        return buf.toString();
    }
}
