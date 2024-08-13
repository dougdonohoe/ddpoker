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

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;

import java.util.*;

public class AIOutcome
{
    public static final int FOLD = 0;
    public static final int CHECK = 0;
    public static final int CALL = 1;
    public static final int BET = 2;
    public static final int RAISE = 2;
    public static final int RERAISE = 2;

    private ArrayList tuples_ = new ArrayList();

    private float checkFold;
    private float call;
    private float betRaise;

    private boolean computed_ = false;

    private PokerPlayer player_;
    private BetRange betRange_;
    private int potStatus_;
    private int round_;
    private boolean isLimit_;
    private String allInReason_;

    private class Tuple
    {
        public int outcome;
        public String tactic;
        public float checkFold;
        public float call;
        public float betRaise;

        public Tuple(int outcome, String tactic, float checkFold, float call, float betRaise)
        {
            this.outcome = outcome;
            this.tactic = tactic;
            this.checkFold = checkFold;
            this.call = call;
            this.betRaise = betRaise;
        }
    }

    public AIOutcome(HoldemHand hhand, PokerPlayer player)
    {
        player_ = player;
        round_ = hhand.getRound();
        potStatus_ = hhand.getPotStatus();
        isLimit_ = hhand.isLimit();
    }

    public void addTuple(int outcome, String tactic, float checkFold, float call, float betRaise)
    {
        tuples_.add(new Tuple(outcome, tactic, checkFold, call, betRaise));

        computed_ = false;
    }

    public void setBetRange(BetRange betRange, String allInReason)
    {
        betRange_ = betRange;
        allInReason_ = allInReason;
    }

    public String toHTML()
    {
        return toHTML(0);
    }

    /**
     * 0- everything
     * 1- omit 'Recommend'
     * 2- omit reasons
     * 3- omit bet amount
     * 5- omit percents
     * @param brevity
     * @return
     */
    public String toHTML(int brevity)
    {
        StringBuilder buf = new StringBuilder();

        Tuple tuple;

        boolean found;

        if (!computed_) computeAverageTuple();

        if (checkFold > 0f)
        {
            if (brevity < 1) buf.append("Recommend ");

            if (potStatus_ == PokerConstants.NO_POT_ACTION)
            {
                buf.append("Check");
            }
            else
            {
                buf.append("Fold");
            }
            if (brevity < 5)
            {
                buf.append(" ");
                buf.append(PokerConstants.formatPercent(checkFold*100f));
                buf.append("%");
            }
            if (brevity < 2)
            {
                found = false;

                for (int i = 0; i < tuples_.size(); ++i)
                {
                    tuple = (Tuple)tuples_.get(i);

                    if (tuple.outcome == AIOutcome.CHECK || tuple.outcome == AIOutcome.FOLD)
                    {
                        if (!found)
                        {
                            buf.append(" (");
                        }
                        else
                        {
                            buf.append(", ");
                        }

                        buf.append(tuple.tactic);

                        found = true;
                    }
                }

                if (found)
                {
                    buf.append(")");
                }
            }
        }

        if (call > 0f)
        {
            if (buf.length() > 0)
            {
                buf.append(" or ");
            }
            else
            {
                if (brevity < 1) buf.append("Recommend ");
            }

            buf.append("Call");
            if (brevity < 5)
            {
                buf.append(" ");
                buf.append(PokerConstants.formatPercent(call*100f));
                buf.append("%");
            }

            found = false;

            if (brevity < 2)
            {
                for (int i = 0; i < tuples_.size(); ++i)
                {
                    tuple = (Tuple)tuples_.get(i);

                    if (tuple.outcome == AIOutcome.CALL)
                    {
                        if (!found)
                        {
                            buf.append(" (");
                        }
                        else
                        {
                            buf.append(", ");
                        }

                        buf.append(tuple.tactic);

                        found = true;
                    }

                }

                if (found)
                {
                    buf.append(")");
                }
            }
        }

        if (betRaise > 0f)
        {
            if (buf.length() > 0)
            {
                buf.append(" or ");
            }
            else
            {
                if (brevity < 1) buf.append("Recommend ");
            }

            switch (potStatus_)
            {
                case PokerConstants.NO_POT_ACTION :
                    if (round_ == HoldemHand.ROUND_PRE_FLOP)
                        buf.append("Raise");
                    else
                        buf.append("Bet");
                    break;
                case PokerConstants.RAISED_POT:
                    buf.append("Raise");
                    break;
                default:
                    buf.append("Re-Raise");
                    break;
            }

            if (brevity < 5)
            {
                buf.append(" ");
                buf.append(PokerConstants.formatPercent(betRaise*100f));
                buf.append("%");
            }

            if ((brevity < 3) && !isLimit_ && (betRange_ != null))
            {
                buf.append(" ");
                buf.append(betRange_.toString(player_, false));
                //buf.append(" Chips");
            }

            if (allInReason_ != null)
            {
                buf.append(" All In");
                if (brevity < 2)
                {
                    buf.append(" (");
                    buf.append(allInReason_);
                    buf.append(")");
                }
            }

            if (brevity < 2)
            {
                found = false;

                for (int i = 0; i < tuples_.size(); ++i)
                {
                    tuple = (Tuple)tuples_.get(i);

                    if (tuple.outcome == AIOutcome.BET || tuple.outcome == AIOutcome.RAISE)
                    {
                        if (!found)
                        {
                            buf.append(" (");
                        }
                        else
                        {
                            buf.append(", ");
                        }

                        buf.append(tuple.tactic);

                        found = true;
                    }
                }

                if (found)
                {
                    buf.append(")");
                }
            }
        }

        return buf.toString();
    }

    public float getCheckFold()
    {
        if (!computed_) computeAverageTuple();

        return checkFold;
    }

    public float getCall()
    {
        if (!computed_) computeAverageTuple();

        return call;
    }

    public float getBetRaise()
    {
        if (!computed_) computeAverageTuple();

        return betRaise;
    }

    public int getStrongestOutcome(int potStatus)
    {
        if (!computed_) computeAverageTuple();

        if (checkFold > call)
        {
            if (checkFold > betRaise)
            {
                if (potStatus == PokerConstants.NO_POT_ACTION)
                {
                    return RuleEngine.OUTCOME_CHECK;
                }
                else
                {
                    return RuleEngine.OUTCOME_FOLD;
                }
            }
        }
        else
        {
            if (call > betRaise)
            {
                return RuleEngine.OUTCOME_CALL;
            }
        }

        if (potStatus == PokerConstants.NO_POT_ACTION)
        {
            if (round_ == HoldemHand.ROUND_PRE_FLOP)
                return RuleEngine.OUTCOME_OPEN_POT;
            else
                return RuleEngine.OUTCOME_BET;
        }
        else
        {
            return RuleEngine.OUTCOME_RAISE;
        }
    }

    public int selectOutcome(int potStatus)
    {
        if (!computed_) computeAverageTuple();

        float v = (float)Math.random();

        if (v < checkFold)
        {
            if (potStatus == PokerConstants.NO_POT_ACTION)
            {
                return RuleEngine.OUTCOME_CHECK;
            }
            else
            {
                return RuleEngine.OUTCOME_FOLD;
            }

        }
        else if (v < checkFold+call)
        {
            return RuleEngine.OUTCOME_CALL;
        }
        else
        {
            if (potStatus == PokerConstants.NO_POT_ACTION)
            {
                return RuleEngine.OUTCOME_BET;
            }
            else
            {
                return RuleEngine.OUTCOME_RAISE;
            }
        }
    }

    private void computeAverageTuple()
    {
        Tuple tuple;

        checkFold = 0f;
        call = 0f;
        betRaise = 0f;

        for (int i = 0; i < tuples_.size(); ++i)
        {
            tuple = (Tuple)tuples_.get(i);

            checkFold += tuple.checkFold;
            call += tuple.call;
            betRaise += tuple.betRaise;
        }

        checkFold /= tuples_.size();
        call /= tuples_.size();
        betRaise /= tuples_.size();

        computed_ = true;
    }
}
