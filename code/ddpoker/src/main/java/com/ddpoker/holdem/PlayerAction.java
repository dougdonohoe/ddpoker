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
package com.ddpoker.holdem;

public class PlayerAction
{
    // FIX: move HandAction values here?
    public static final int FOLD = 0;  // HandAction.ACTION_FOLD;
    public static final int CHECK = 1; // HandAction.ACTION_CHECK;
    public static final int CALL = 3;  //HandAction.ACTION_CALL;
    public static final int BET = 4;   //HandAction.ACTION_BET;
    public static final int RAISE = 5; // HandAction.ACTION_RAISE;

    private int fold_ = 0;
    private int check_ = 0;
    private int call_ = 0;
    private int bet_ = 0;

    private int rand_;
    private int action_;

    private String sDebug_;

    PlayerAction(int fold, int check, int call, int bet)
    {
        fold_ = fold;
        check_ = check;
        call_ = call;
        bet_ = bet;

        rand_ = (int)(Math.random() * 101);

        // TODO: identify from context what's legal (esp bet vs raise)
        if (rand_ < bet_)
        {
            action_ = BET;
        }
        else if (rand_ < bet_ + call_)
        {
            action_ = CALL;
        }
        else if (rand_ < bet_ + call_ + check_)
        {
            action_ = CHECK;
        }
        else if (rand_ < bet_ + call_ + check_ + fold_)
        {
            action_ = FOLD;
        }
        else
        {
            action_ = FOLD;
        }
    }

    public int getType()
    {
        return action_;
    }

    public boolean isFold()
    {
        return (action_ == FOLD);
    }

    public boolean isCheck()
    {
        return (action_ == CHECK);
    }

    public boolean isCall()
    {
        return (action_ == CALL);
    }

    public boolean isBet()
    {
        return (action_ == BET);
    }

    public boolean isRaise()
    {
        return (action_ == RAISE);
    }

    public PlayerAction reason(String text)
    {
        sDebug_ = text;
        return this;
    }

    public String getReason()
    {
        return sDebug_;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();

        if (fold_ > 0)
        {
            buf.append("fold ");
            buf.append(fold_);
            buf.append("%, ");
        }
        if (check_ > 0)
        {
            buf.append("check ");
            buf.append(check_);
            buf.append("%, ");
        }
        if (call_ > 0)
        {
            buf.append("call ");
            buf.append(call_);
            buf.append("%, ");
        }
        if (bet_ > 0)
        {
            buf.append("bet ");
            buf.append(bet_);
            buf.append("%, ");
        }
        buf.setLength(buf.length()-2);
        switch (action_)
        {
            case FOLD:
                return "Fold (" + buf.toString() + "): " + getReason();
            case CHECK:
                return "Check (" + buf.toString() + "): " + getReason();
            case CALL:
                return "Call (" + buf.toString() + "): " + getReason();
            case BET:
                return "Bet (" + buf.toString() + "): " + getReason();
            case RAISE:
                return "Raise (" + buf.toString() + "): " + getReason();
            default:
                return "No Action (" + buf.toString() + "): " + getReason();
        }
    }

    public int getFoldProbability()
    {
        return fold_;
    }

    public int getCheckProbability()
    {
        return check_;
    }

    public int getCallProbability()
    {
        return call_;
    }

    public int getBetProbability()
    {
        return bet_;
    }

    public static PlayerAction fold()
    {
        return new PlayerAction(100, 0, 0, 0);
    }

    public static PlayerAction check()
    {
        return new PlayerAction(0, 100, 0, 0);
    }

    public static PlayerAction call()
    {
        return new PlayerAction(0, 0, 100, 0);
    }

    public static PlayerAction bet()
    {
        return new PlayerAction(0, 0, 0, 100);
    }

    public static PlayerAction raise()
    {
        return new PlayerAction(0, 0, 0, 100);
    }

    public static PlayerAction act(int fold, int check, int call, int bet)
    {
        return new PlayerAction(fold, check, call, bet);
    }
}
