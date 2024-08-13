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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import java.math.*;
import java.util.*;

public class HoldemSimulator
{
    static Logger logger = Logger.getLogger(HoldemSimulator.class);

    // defines
    private static final int DEFAULT_PRECISION = 4; // TODO: this should be replaced by an option
    public static final String RANDOM_HANDS = PropertyConfig.getMessage("msg.sim.random");
    public static final String ALL_HANDS = PropertyConfig.getMessage("msg.sim.all");

    /**
     * get interval to update - divide handCount by given number and
     * return result - updates happen every [result] loops.  If the
     * handCount is less than the divide by, return interval of 1/2
     * handcount
     */
    private static int getInterval(int handCount, int nDivideBy)
    {
        if (nDivideBy >= handCount)
        {
            if (handCount <= 2) return 1;
            else return handCount / 2;
        }
        int nInterval = handCount / nDivideBy;
        if (nInterval > 50000) nInterval = 50000;
        return nInterval;
    }

    /**
     * simulate with default precision
     */
    public static StatResults simulate(Hand hole, Hand community, DDProgressFeedback progress)
    {
        return simulate(hole, community, DEFAULT_PRECISION, progress);
    }

    /**
     * Simulate logic
     */
    public static StatResults simulate(Hand hole, Hand community, int precision, DDProgressFeedback progress)
    {
        Deck deck = new Deck(true);
        deck.removeCards(hole);
        if (community != null)
        {
            deck.removeCards(community);
        }

        StatResults results = new StatResults();
        List<BaseProfile> handGroups = HandGroup.getProfileList();
        HandGroup group;
        HandList list;
        int handCount;
        int nNumGroups = handGroups.size();
        int nNumSims = nNumGroups + 1;
        int nNumDone = 0;
        int nSize = community.size();

        // hand groups
        for (int i = 0; i < nNumGroups && ((progress == null) || !progress.isStopRequested()); ++i)
        {
            group = (HandGroup) handGroups.get(i);
            msg(progress, group.getName());
            list = group.expand();

            // if we have flop, we can iterate over remaining cards
            if (nSize >= 3)
            {
                handCount = list.size() * nCm(52 - 2 - 2 - nSize, 5 - nSize).intValue();
                results.put(group, iterate(hole, community, handCount, deck, list, progress, nNumDone, nNumSims));
            }
            else
            {
                handCount = (int) (Math.pow(2, precision) * (Math.log(list.size()) + 0.5) * 1000);
                results.put(group, simulate(hole, community, handCount, deck, list, progress, nNumDone, nNumSims));
            }
            nNumDone++;
            perc(progress, nNumDone, nNumSims);
            if (progress != null) progress.setIntermediateResult(results);
        }

        // all hands (3+ boards) or random hands
        if ((progress != null) && !progress.isStopRequested())
        {
            // do all hands when we have 4 or 5 board cards
            // we don't do 3 because it is just over a million hands, which is a bit much
            // and they can use the showdown simulator for that
            if (nSize >= 4)
            {
                msg(progress, RANDOM_HANDS);
                list = HandGroup.getAllHands().expand();
                handCount = list.size() * nCm(52 - 2 - 2 - nSize, 5 - nSize).intValue();
                results.put(RANDOM_HANDS, iterate(hole, community, handCount, deck, list, progress, nNumDone, nNumSims));
            }
            else
            {
                msg(progress, RANDOM_HANDS);
                handCount = 100000;
                results.put(RANDOM_HANDS, simulate(hole, community, handCount, deck, null, progress, nNumDone, nNumSims));
            }
            nNumDone++;
            perc(progress, nNumDone, nNumSims);
        }

        if (progress != null)
        {
            progress.setFinalResult(results);
        }
        return results;
    }

    /**
     * N choose M      N!
     * -----------
     * M! * (N-M)!
     */
    public static BigInteger nCm(int n, int m)
    {
        if (m > n) return BigInteger.valueOf(0);
        if (m == n || m == 0) return BigInteger.valueOf(1);
        if (m == 1) return BigInteger.valueOf(n);

        BigInteger nFacDivNminusMFac = BigInteger.valueOf(1);

        for (int i = n; i >= (n - m + 1); i--)
        {
            nFacDivNminusMFac = nFacDivNminusMFac.multiply(BigInteger.valueOf(i));
        }

        BigInteger mFac = BigInteger.valueOf(1);
        for (int i = 2; i <= m; i++)
        {
            mFac = mFac.multiply(BigInteger.valueOf(i));
        }

        return nFacDivNminusMFac.divide(mFac);
    }

    /**
     * progress status message - hand group we are running
     */
    private static void msg(DDProgressFeedback progress, String sName)
    {
        if (progress != null)
        {
            progress.setMessage(PropertyConfig.getMessage("msg.sim.running", sName));
        }
    }

    /**
     * progress percent done - when compeleted a hand group
     */
    private static void perc(DDProgressFeedback progress, long nNum, long nTotal)
    {
        if (progress != null)
        {
            double total = nNum / (double) nTotal * 100.0d;
            progress.setPercentDone((int) total);
        }
    }

    /**
     * progress percent done - during a hand group
     */
    private static void perc(DDProgressFeedback progress, int nNum, int nTotal, int nInc, int nIncTotal)
    {
        if (progress != null)
        {
            double inc = nInc / (double) nIncTotal;
            double total = (nNum + inc) / (double) nTotal * 100.0d;
            progress.setPercentDone((int) total);
        }
    }

    /**
     * simulate logic
     */
    public static StatResult simulate(Hand hole, Hand community, int handCount, Deck deck, HandList list,
                                      DDProgressFeedback progress, int nNumDone, int nNumSims)
    {

        int deckSize = deck.size();

        Hand against;
        Hand trialCommunity;

        //logger.debug("Simulating " + hole + " vs. " + ((list == null) ? "random hands": list.getName()) + " ("+
        //                            handCount + " hands).");

        int win = 0;
        int lose = 0;
        int tie = 0;

        HandInfoFaster fast = new HandInfoFaster();

        boolean randomHand = (list == null);

        int updateBarInterval = getInterval(handCount, 100);
        int opponentCount = randomHand ? 0 : list.size();
        int handsPerOpponent = randomHand ? 0 : Math.max(handCount / opponentCount, 1);

        for (int i = 0; i < handCount; ++i)
        {
            if (i % updateBarInterval == 0)
            {
                perc(progress, nNumDone, nNumSims, i, handCount);
            }

            if (randomHand)
            {
                against = new Hand(deck.nextCard(), deck.nextCard());
            }
            else
            {
                int opponentIndex = i / handsPerOpponent;
                if (opponentIndex >= opponentCount)
                {
                    break;
                }
                against = list.get(opponentIndex);
                if (hole.containsAny(against) || ((community != null) && (community.containsAny(against))))
                {
                    i = i + handsPerOpponent - 1;
                    continue;
                }
                deck.removeCards(against);
            }

            if (community == null)
            {
                trialCommunity = new Hand();
            }
            else
            {
                trialCommunity = new Hand(community);
            }

            while (trialCommunity.size() < 5)
            {
                trialCommunity.addCard(deck.nextCard());
            }

            //System.out.println("Getting score for player " + hole + trialCommunity);
            //System.out.println("Getting score for opponent " + against + trialCommunity);

            int result = fast.getScore(hole, trialCommunity) - fast.getScore(against, trialCommunity);

            /*

            // DEBUG

            int f = fast.getScore(hole, trialCommunity);
            int s = new HandInfo(hole, trialCommunity).getScore();

            if (f != s) {
                System.out.println("Fast score " + f + " differs from slow score " + s + " for " + hole + trialCommunity);
                System.out.println("Fast says " + HandInfo.getHandTypeDesc(HandInfoFast.getTypeFromScore(f)));
                System.out.println("Slow says " + HandInfo.getHandTypeDesc(HandInfoFast.getTypeFromScore(s)));
            }

            System.out.println("Fast says " + HandInfo.getHandTypeDesc(HandInfoFast.getTypeFromScore(f)) + " for " + hole + trialCommunity);
            */

            if (result == 0)
            {
                ++tie;
            }
            else if (result < 0)
            {
                ++lose;
            }
            else
            {
                ++win;
            }

            deck.addRandom(against);

            if (community != null)
            {
                trialCommunity.removeAll(community);
            }

            // System.out.println("Adding back " + trialCommunity);

            deck.addRandom(trialCommunity);
        }

        if (deckSize != deck.size())
        {
            logger.warn("Deck size mismatch in simulator!  Before, size was " + deckSize + ", after size was " + deck.size());
        }

        return new StatResult(hole, list, win, lose, tie);
    }

    /**
     * iterate logic
     */
    public static StatResult iterate(Hand hole, Hand community, int estHandCount, Deck deck, HandList list,
                                     DDProgressFeedback progress, int nNumDone, int nNumSims)
    {
        int nComm = community != null ? community.size() : 0;
        int MORE = 5 - nComm;

        // too expensive to calculate all 5 card boards, so just estimate
        // from the flop ... TODO: calling with MORE==5 isn't used, so we could
        // still decide to iterate over all
        if (MORE > 3) MORE = 3;

        // tally
        Results results = new Results();

        // init
        HandInfoFaster FAST = new HandInfoFaster();
        Hand commcopy = new Hand(community); // copy for reuse

        // get remaining cards (new deck less hole, community)
        Hand against;
        for (int i = 0; i < list.size() && ((progress == null) || !progress.isStopRequested()); i++)
        {
            against = list.get(i);

            // if opponent's hand contains cards already in use, skip
            // this will cause total count to be less than estimated handCount,
            // but that is only used for progress display
            if (hole.containsAny(against) || ((community != null) && (community.containsAny(against))))
            {
                continue;
            }

            // remove opponents cards from the deck
            deck.removeCards(against);

            // more init
            int nSize = deck.size();

            // loop over all remaining board cards
            if (MORE >= 1)
            {
                for (int next1 = 0; next1 < (nSize - (MORE - 1)) && ((progress == null) || !progress.isStopRequested()); next1++)
                {
                    commcopy.addCard(deck.getCard(next1));
                    if (MORE >= 2)
                    {
                        for (int next2 = next1 + 1; next2 < (nSize - (MORE - 2)) && ((progress == null) || !progress.isStopRequested()); next2++)
                        {
                            commcopy.addCard(deck.getCard(next2));
                            if (MORE >= 3)
                            {
                                for (int next3 = next2 + 1; next3 < (nSize - (MORE - 3)) && ((progress == null) || !progress.isStopRequested()); next3++)
                                {
                                    commcopy.addCard(deck.getCard(next3));
                                    if (MORE >= 4)
                                    {
                                        for (int next4 = next3 + 1; next4 < (nSize - (MORE - 4)) && ((progress == null) || !progress.isStopRequested()); next4++)
                                        {
                                            commcopy.addCard(deck.getCard(next4));
                                            if (MORE == 5)
                                            {
                                                for (int next5 = next4 + 1; next5 < nSize && ((progress == null) || !progress.isStopRequested()); next5++)
                                                {
                                                    commcopy.addCard(deck.getCard(next5));
                                                    score(FAST, hole, against, commcopy, results, progress, nNumDone, nNumSims, estHandCount);
                                                    commcopy.removeCard(commcopy.size() - 1);
                                                }
                                            }
                                            else
                                            {
                                                score(FAST, hole, against, commcopy, results, progress, nNumDone, nNumSims, estHandCount);
                                            }
                                            commcopy.removeCard(commcopy.size() - 1);
                                        }
                                    }
                                    else
                                    {
                                        score(FAST, hole, against, commcopy, results, progress, nNumDone, nNumSims, estHandCount);
                                    }
                                    commcopy.removeCard(commcopy.size() - 1);
                                }
                            }
                            else
                            {
                                score(FAST, hole, against, commcopy, results, progress, nNumDone, nNumSims, estHandCount);
                            }
                            commcopy.removeCard(commcopy.size() - 1);
                        }
                    }
                    else
                    {
                        score(FAST, hole, against, commcopy, results, progress, nNumDone, nNumSims, estHandCount);
                    }
                    commcopy.removeCard(commcopy.size() - 1);
                }
            }
            else
            {
                score(FAST, hole, against, commcopy, results, progress, nNumDone, nNumSims, estHandCount);
            }

            // put them back
            deck.addRandom(against);
        }

        return new StatResult(hole, list, results.win, results.lose, results.tie);
    }

    /**
     * Record score from iterator and update progress meter
     */
    private static void score(HandInfoFaster fast, Hand hole, Hand against, Hand community, Results results,
                              DDProgressFeedback progress, int nNumDone, int nNumSims, int handCount)
    {
        int result = fast.getScore(hole, community) - fast.getScore(against, community);

        if (result == 0)
        {
            ++results.tie;
        }
        else if (result < 0)
        {
            ++results.lose;
        }
        else
        {
            ++results.win;
        }

        if (progress != null && results.getTotal() % getInterval(handCount, 100) == 0)
        {
            perc(progress, nNumDone, nNumSims, results.getTotal(), handCount);
        }
    }


    /**
     * hold results
     */
    private static class Results
    {
        int win;
        int lose;
        int tie;

        private int getTotal()
        {
            return win + lose + tie;
        }
    }


    ////
    //// Items below go together - not sure what is the purpose since Sam doesn't comment his code
    ////

    /**
     * Simulate active hands in given HoldemHand over handCount trials, replacing
     * missing or blank cards with random cards from the deck
     */
    public static StatResult[] simulate(HoldemHand hhand, int handCount)
    {
        PokerTable table = hhand.getTable();
        PokerPlayer player;

        Hand hands[] = new Hand[10];

        for (int seat = 0; seat < 10; ++seat)
        {
            player = table.getPlayer(seat);

            if ((player == null) || player.isFolded())
            {
                hands[seat] = null;
            }
            else
            {
                hands[seat] = new Hand(player.getHand());
                hands[seat].removeBlank();
            }
        }

        Hand community = new Hand(hhand.getCommunity());
        community.removeBlank();

        return simulate(hands, community, handCount, null);
    }


    /**
     * Simulate handCount trials of given number of hands and community, replacing
     * missing cards with cards random cards from the deck.  NOTE: calling with
     * blank cards in hand/community will cause errors.
     */
    public static StatResult[] simulate(Hand[] hands, Hand community, int handCount,
                                        DDProgressFeedback progress)
    {
        //long now = System.currentTimeMillis();
        Deck deck = new Deck(true);

        StatResult[] results = new StatResult[hands.length];

        for (int i = 0; i < hands.length; ++i)
        {
            if (hands[i] != null)
            {
                deck.removeCards(hands[i]);
                results[i] = new StatResult();
            }
        }

        if (community != null)
        {
            deck.removeCards(community);
        }

        int deckSize = deck.size();

        Hand[] trialHands = new Hand[hands.length];
        Hand trialCommunity;

        HandInfoFaster fast = new HandInfoFaster();

        Hand dealt = new Hand();
        int updateResultsInterval = getInterval(handCount, 25);
        int updateBarInterval = getInterval(handCount, 100);

        for (int i = 0; i < handCount && ((progress == null) || !progress.isStopRequested()); ++i)
        {
            // mark progress / look for stop
            if (progress != null)
            {
                if (i % updateBarInterval == 0 || i == (handCount - 1)) perc(progress, i + 1, handCount);
                if (i % updateResultsInterval == 0)
                {
                    calcResults(results);
                    progress.setIntermediateResult(results);
                }
                if (progress.isStopRequested())
                {
                    handCount = i;
                    break;
                }
            }

            // complete any hands that need it
            for (int j = 0; j < hands.length; ++j)
            {
                if (hands[j] == null)
                {
                    trialHands[j] = null;
                }
                else
                {
                    trialHands[j] = hands[j].size() == 2 ? hands[j] : new Hand(hands[j]);
                    while (trialHands[j].size() < 2)
                    {
                        Card c = deck.nextCard();
                        dealt.addCard(c);
                        trialHands[j].addCard(c);
                    }
                }
            }

            // create community
            if (community == null)
            {
                trialCommunity = new Hand();
            }
            else
            {
                trialCommunity = community.size() == 5 ? community : new Hand(community);
            }

            // add community cards
            while (trialCommunity.size() < 5)
            {
                Card c = deck.nextCard();
                dealt.addCard(c);
                trialCommunity.addCard(c);
            }

            score(trialHands, fast, trialCommunity, results);

            // put cards back in deck
            deck.addRandom(dealt);
            dealt.clear();
        }

        // safety check
        if (deckSize != deck.size())
        {
            logger.warn("Deck size mismatch in simulator!  Before, size was " + deckSize + ", after size was " + deck.size());
        }

        // calc total at end
        calcResults(results);

        // finalize result
        if (progress != null)
        {
            progress.setFinalResult(results);
        }
        //logger.debug("Sim took: " + ((System.currentTimeMillis() - now) /1000.0d) + " seconds");
        return results;
    }

    /**
     * figure out winner/loser and store results.
     */
    private static void score(Hand[] trialHands, HandInfoFaster fast, Hand trialCommunity, StatResult[] results)
    {
        int winners = 0;
        int winnerCount = 0;
        int winningScore = 0;

        // figure out winner(s) - loop based on results.length since trialHands could have
        // extra entries
        for (int j = 0; j < results.length; ++j)
        {
            if (trialHands[j] != null)
            {
                int score = fast.getScore(trialHands[j], trialCommunity);

                if (score > winningScore)
                {
                    winningScore = score;
                    winners = 1 << j;
                    winnerCount = 1;
                }
                else if (score == winningScore)
                {
                    winners |= 1 << j;
                    ++winnerCount;
                }
            }
        }

        // mark winner
        for (int j = 0; j < results.length; ++j)
        {
            if (trialHands[j] != null)
            {
                if ((winners & 1 << j) == 0)
                {
                    results[j].addLose(false);
                }
                else if (winnerCount > 1)
                {
                    results[j].addTie(false);
                }
                else
                {
                    results[j].addWin(false);
                }
            }
        }
    }

    /**
     * calculate totals
     */
    private static void calcResults(StatResult[] results)
    {
        for (StatResult result : results)
        {
            if (result != null)
            {
                result.calcTotal();
            }
        }
    }

    /**
     * Figure out number of iterations to simulate all possible hands
     */
    public static BigInteger getNumberIterations(Hand[] hands, Hand community)
    {
        BigInteger num = BigInteger.valueOf(1);
        int nCards = 52;
        int nBlank;

        // figure out number of cards dealt
        for (Hand hand : hands)
        {
            nCards -= hand.size() - hand.countCard(Card.BLANK);
        }
        nCards -= community.size() - community.countCard(Card.BLANK);

        // iterations per hand
        for (Hand hand : hands)
        {
            nBlank = hand.countCard(Card.BLANK);
            num = num.multiply(nCm(nCards, nBlank));
            nCards -= nBlank;
        }

        // iterations for board cards
        nBlank = community.countCard(Card.BLANK);
        num = num.multiply(nCm(nCards, nBlank));

        return num;
    }

    /**
     * iterate all results - all hands must be complete - missing cards should be Card.BLANK
     */
    public static StatResult[] iterate(Hand[] hands, Hand community, DDProgressFeedback progress)
    {
        // init
        IndexKeeper ik = new IndexKeeper();
        HandInfoFaster fast = new HandInfoFaster();
        Deck deck = new Deck(false);
        StatResult results[] = new StatResult[hands.length];

        // remove dealt cards from deck
        for (int i = 0; i < results.length; i++)
        {
            results[i] = new StatResult();
            deck.removeCards(hands[i]);
        }
        deck.removeCards(community);

        // create array of all hands plus community at end
        Hand allhands[] = new Hand[hands.length + 1];
        System.arraycopy(hands, 0, allhands, 0, hands.length);
        allhands[hands.length] = community;

        // figure out how many we are doing for progress bar
        ik.todo = getNumberIterations(hands, community);
        if (ik.todo.toString().length() < 9)
        {
            int handCount = ik.todo.intValue();
            ik.updateResultsInterval = getInterval(handCount, 25);
            ik.updateBarInterval = getInterval(handCount, 100);
        }

        // do the work
        iterate(fast, results, deck, allhands, progress, ik, 0);

        // calc total at end
        calcResults(results);

        // finalize result
        if (progress != null)
        {
            progress.setFinalResult(results);
        }
        return results;
    }

    /**
     * recursive algorithm to iterate through all combinations
     */
    private static void iterate(HandInfoFaster fast, StatResult results[],
                                Deck deck, Hand[] allhands,
                                DDProgressFeedback progress,
                                IndexKeeper ik, int nDeckStartIdx)
    {
        if ((progress != null) && progress.isStopRequested()) return;

        int nHandIdxAtStart = ik.nHandIdx;
        ik.nextIndex(allhands);
        if (nHandIdxAtStart != ik.nHandIdx)
        {
            nDeckStartIdx = 0;
        }

        if (ik.nHandIdx == DONE)
        {
            // calc score
            ik.nNum++;
            score(allhands, fast, allhands[allhands.length - 1], results);

            // update progress
            if (progress != null)
            {
                BigInteger handCount = ik.todo;
                BigInteger bigNum = BigInteger.valueOf(ik.nNum);
                if (ik.nNum % ik.updateBarInterval == 0 || bigNum.equals(handCount))
                {
                    BigInteger total = bigNum.multiply(BigInteger.valueOf(100)).divide(handCount);
                    if (progress != null) progress.setPercentDone(total.intValue());
                }
                if (ik.nNum % ik.updateResultsInterval == 0)
                {
                    calcResults(results);
                    if (progress != null) progress.setIntermediateResult(results);
                }
            }

            return;
        }

        int nHandIdx = ik.nHandIdx;
        int nCardIdx = ik.nCardIdx;
        int nNumCards = deck.size();
        Card nextCard;
        Card handCard;

        for (int i = nDeckStartIdx; i < nNumCards && ((progress == null) || !progress.isStopRequested()); i++)
        {
            nextCard = deck.getCard(i);
            if (nextCard.isBlank()) continue;


            handCard = allhands[nHandIdx].getCard(nCardIdx);
            //logger.debug("Assigning hand " + nHandIdx + "["+ nCardIdx +"] to " + nextCard);
            ApplicationError.assertTrue(handCard.equals(Card.BLANK), "Should be blank card", handCard);
            handCard.setValue(nextCard);
            nextCard.setValue(Card.BLANK);

            iterate(fast, results, deck, allhands, progress, ik, i + 1);

            nextCard.setValue(handCard);
            handCard.setValue(Card.BLANK);
            ik.nHandIdx = nHandIdx;
            ik.nCardIdx = nCardIdx;
        }
    }

    private static int DONE = -2;
    private static int INIT = -1;

    private static class IndexKeeper
    {
        int nHandIdx = INIT;
        int nCardIdx = INIT;
        long nNum;
        BigInteger todo;
        int updateResultsInterval = 50000;
        int updateBarInterval = 10000;

        void nextIndex(Hand allhands[])
        {
            if (nHandIdx == INIT)
            {
                nextHand(allhands);
                return;
            }

            if (!nextCard(allhands))
            {
                nextHand(allhands);
            }
        }

        private void nextHand(Hand allhands[])
        {
            while (true)
            {
                nCardIdx = INIT;
                nHandIdx++;
                if (nHandIdx >= allhands.length)
                {
                    nHandIdx = DONE;
                    return;
                }
                if (nextCard(allhands))
                {
                    return;
                }
            }
        }

        /**
         * return true if index incremented and still in current hand
         */
        private boolean nextCard(Hand allhands[])
        {
            Card c;
            boolean bDone = false;
            while (!bDone)
            {
                nCardIdx++;
                if (nCardIdx >= allhands[nHandIdx].size())
                {
                    return false;
                }
                c = allhands[nHandIdx].getCard(nCardIdx);
                if (c.equals(Card.BLANK))
                {
                    bDone = true;
                }
            }
            return true;
        }
    }
}
