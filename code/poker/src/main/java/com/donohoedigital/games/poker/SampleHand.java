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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Deck;

import java.util.ArrayList;
import java.util.List;

/**
 * A scripted hand (see {@link SampleHands}), played through the real PokerPlayer and HoldemHand
 * code (blinds, betting, side pots, showdown) and stored with the real storeHandHistory().
 * <p>
 * The script is a list of actions, applied in turn to whoever is to act:
 * <ul>
 * <li>{@code f} fold, {@code x} check, {@code c} call</li>
 * <li>{@code b100} bet 100, {@code r100} raise by 100 (on top of the call)</li>
 * <li>{@code a} all-in (bet, raise or call, whichever all the player's chips make)</li>
 * <li>{@code |} end of a betting round (checked, then the next card(s) dealt)</li>
 * </ul>
 * After the script, a hand still contested only because everyone is all-in is run out to the river.
 * Hole cards and the board are stacked, so the winner is known.
 */
public class SampleHand
{
    static final int SMALL_BLIND = 10;
    static final int BIG_BLIND = 20;

    private final String name_;
    private int button_;
    private int[] stacks_;
    private String[] holes_;
    private String board_;
    private int ante_;
    private String script_;
    private int[] expected_;

    SampleHand(String name)
    {
        name_ = name;
    }

    String getName()
    {
        return name_;
    }

    /**
     * Seat with the button.  Three-handed, the button acts first pre-flop and last after.
     */
    SampleHand button(int seat)
    {
        button_ = seat;
        return this;
    }

    /**
     * Chips at the start of the hand, by seat
     */
    SampleHand stacks(int... stacks)
    {
        stacks_ = stacks;
        return this;
    }

    /**
     * Hole cards by seat, e.g. "AhKh"
     */
    SampleHand holes(String... holes)
    {
        holes_ = holes;
        return this;
    }

    /**
     * Five board cards, e.g. "Ah7d2s Kc 3d" (spaces ignored)
     */
    SampleHand board(String board)
    {
        board_ = board.replace(" ", "");
        return this;
    }

    SampleHand ante(int ante)
    {
        ante_ = ante;
        return this;
    }

    /**
     * The actions (see class comment)
     */
    SampleHand script(String script)
    {
        script_ = script;
        return this;
    }

    /**
     * Chips by seat once the pot is awarded (checked by play())
     */
    SampleHand expectChips(int... chips)
    {
        expected_ = chips;
        return this;
    }

    /**
     * Play the script on the table (whose players are in seats 0..n-1) and store it.
     * Returns the stored hand's ID.
     */
    int play(PokerTable table)
    {
        check(stacks_.length == holes_.length, "stacks and holes differ in length");
        for (int seat = 0; seat < stacks_.length; seat++)
        {
            table.getPlayerRequired(seat).setChipCount(stacks_[seat]);
        }
        table.setButton(button_);

        HoldemHand hhand = new HoldemHand(table);
        table.setHoldemHand(hhand);
        hhand.setSmallBlind(SMALL_BLIND);
        hhand.setBigBlind(BIG_BLIND);
        hhand.setAnte(ante_);
        hhand.setDeck(stackDeck(table));
        hhand.deal();

        for (String action : script_.trim().split("\\s+"))
        {
            if (action.equals("|"))
            {
                check(hhand.isDone(), "round " + hhand.getRound() + " not done at '|'");
                nextRound(hhand);
                continue;
            }

            PokerPlayer player = hhand.getCurrentPlayerInitIndex();
            check(player != null, "nobody to act for '" + action + "' in round " + hhand.getRound());
            act(hhand, player, action);
        }

        // like TournamentDirector, deal the rest of the board even if the hand is over, then show down
        check(hhand.isDone(), "script ended mid-round " + hhand.getRound());
        boolean bContested = !hhand.isUncontested();
        while (hhand.getRound() < HoldemHand.ROUND_RIVER)
        {
            nextRound(hhand);
            // if contested, anything left can only be a run out, with everyone all-in
            if (bContested) check(hhand.getCurrentPlayer() == null, "script ended with action left in round " + hhand.getRound());
        }
        hhand.advanceRound();
        hhand.preResolve(false); // false for practice games
        hhand.resolve();

        if (expected_ != null)
        {
            for (int seat = 0; seat < expected_.length; seat++)
            {
                int chips = table.getPlayerRequired(seat).getChipCount();
                check(chips == expected_[seat], "seat " + seat + " has " + chips + " chips, expected " + expected_[seat]);
            }
        }

        return hhand.storeHandHistory();
    }

    private void check(boolean b, String sDetails)
    {
        ApplicationError.assertTrue(b, name_ + ": " + sDetails);
    }

    private void nextRound(HoldemHand hhand)
    {
        hhand.advanceRound();
        hhand.getCurrentPlayerInitIndex();
    }

    private void act(HoldemHand hhand, PokerPlayer player, String action)
    {
        switch (action.charAt(0))
        {
            case 'f' -> player.fold(reason(player, "fold"), HandAction.FOLD_NORMAL);
            case 'x' -> player.check(reason(player, "check"));
            case 'c' -> player.call(reason(player, "call"));
            case 'b' -> player.bet(Integer.parseInt(action.substring(1)), reason(player, "bet"));
            case 'r' -> player.raise(Integer.parseInt(action.substring(1)), reason(player, "raise"));
            case 'a' ->
            {
                String reason = reason(player, "allin");
                int call = hhand.getCall(player);
                if (call == 0) player.bet(player.getChipCount(), reason);
                else if (call >= player.getChipCount()) player.call(reason);
                else player.raise(player.getChipCount() - call, reason);
                check(player.isAllIn(), action + " left the player with chips");
            }
            default -> check(false, "unknown action " + action);
        }
    }

    /**
     * The reason stored with an action, which "Show Reason" displays (msg.aioutcome.*): the
     * button for a human, as the game records it, or an AI outcome for a computer player
     */
    private static String reason(PokerPlayer player, String action)
    {
        if (player.isHuman()) return action + "btn";
        return action.equals("raise") ? "raisevalue" : action;
    }

    /**
     * Deck in the order HoldemHand deals: two passes around the table from the seat
     * after the button, then burn, flop, burn, turn, burn, river.
     */
    private Deck stackDeck(PokerTable table)
    {
        List<Card> order = new ArrayList<>();
        for (int c = 0; c < 2; c++)
        {
            int seat = table.getNextSeatAfterButton();
            for (int i = 0; i < stacks_.length; i++)
            {
                order.add(Card.getCard(holes_[seat].substring(c * 2, c * 2 + 2)));
                seat = table.getNextSeat(seat);
            }
        }

        Deck deck = new Deck(true, 1);
        for (int i = 0; i < 5; i++)
        {
            deck.remove(Card.getCard(board_.substring(i * 2, i * 2 + 2)));
        }
        for (Card card : order)
        {
            deck.remove(card);
        }

        order.add(deck.nextCard()); // burn
        for (int i = 0; i < 3; i++) order.add(Card.getCard(board_.substring(i * 2, i * 2 + 2)));
        order.add(deck.nextCard()); // burn
        order.add(Card.getCard(board_.substring(6, 8)));
        order.add(deck.nextCard()); // burn
        order.add(Card.getCard(board_.substring(8, 10)));

        for (int i = order.size() - 1; i >= 0; i--)
        {
            deck.moveToTop(order.get(i));
        }
        check(deck.size() == 52, "a card is used twice");
        return deck;
    }
}
