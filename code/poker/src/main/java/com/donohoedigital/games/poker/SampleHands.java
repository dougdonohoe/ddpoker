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

import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.engine.PokerConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Typical hold'em hands, for filling a hand-history database (tests, and the add-sample-hands tool).  Three-handed: you (the human) in seat 0,
 * which is the small blind, the big blind in seat 1, and the button in seat 2.  Pre-flop the button acts
 * first, then you, then the big blind; after the flop it's you, seat 1, seat 2.  Blinds 10/20, 1000 chips.
 */
public class SampleHands
{
    /**
     * Seat a table in the game - the profile's player in seat 0, two computer players in
     * seats 1 and 2 - and play and store every hand, as a new tournament starting now.
     * Returns the stored hand IDs.
     */
    public static List<Integer> playAll(PokerGame game, PlayerProfile profile)
    {
        game.initId();
        PokerTable table = new PokerTable(game, 1);
        table.setMinChip(1);

        // as TournamentOptions creates the human: with this engine's key, so it is stored as
        // the profile's own player (the hand history and statistics screens show only that)
        String key = GameEngine.getGameEngine().getPublicUseKey();
        seat(game, table, 0, new PokerPlayer(key, PokerConstants.PLAYER_ID_HOST, profile, true));
        seat(game, table, 1, new PokerPlayer(1, "Sample Lefty", false));
        seat(game, table, 2, new PokerPlayer(2, "Sample Righty", false));

        List<Integer> ids = new ArrayList<>();
        for (SampleHand hand : all())
        {
            ids.add(hand.play(table));
        }
        return ids;
    }

    private static void seat(PokerGame game, PokerTable table, int seat, PokerPlayer player)
    {
        table.setPlayer(player, seat);
        game.addPlayer(player);
    }

    static List<SampleHand> all()
    {
        return List.of(
                hand("You fold pre-flop").holes("7c2d", "KsKd", "QhJh").board("As8d4c 9h 3s")
                        .script("c f x | x x | x x | x x").expectChips(990, 1030, 980),

                hand("Folded to the big blind").holes("9c4d", "8s8d", "Th2c").board("AcKd7h 5s 3c")
                        .script("f f").expectChips(990, 1010, 1000),

                hand("You raise, bet the flop, everyone folds").holes("AhKd", "9s8s", "Tc7c").board("Ks5d2h Jc 4d")
                        .script("c r40 c c | b100 f f").expectChips(1120, 940, 940),

                hand("You fold on the flop").holes("6h5h", "AcAs", "KhQc").board("Ad9c2s 7d Js")
                        .script("c c x | x b60 c f | x x | x x").expectChips(980, 1100, 920),

                hand("You fold on the turn").holes("JdTd", "QsQh", "8c8h").board("Qd5c2h 9s 3d")
                        .script("c c x | x x x | x b60 c f | x x").expectChips(980, 1100, 920),

                hand("You fold on the river").holes("4s4c", "AdKd", "AhQh").board("Ac7s2d 9c Ts")
                        .script("c c x | x x x | x x x | x b100 f f").expectChips(980, 1040, 980),

                hand("You check-raise the flop").holes("7c7d", "AsJd", "KdTh").board("7sJc2d 4h 9c")
                        .script("c c x | x b60 f r120 c | x x | x x").expectChips(1220, 800, 980),

                hand("You check-raise the turn").holes("8h8d", "AhTc", "6s5s").board("Ac8s3d Kd 2c")
                        .script("c c x | x x x | x b60 f r120 c | x x").expectChips(1220, 800, 980),

                hand("You check-raise the river").holes("QcQd", "KhJh", "9d9c").board("Qs6h3c 2s Kc")
                        .script("c c x | x x x | x x x | x b60 f r120 c").expectChips(1220, 800, 980),

                hand("You three-bet pre-flop").holes("AcAd", "7h6h", "KsKh").board("2c9dJh 4s 5c")
                        .script("r40 r120 f c | b200 f").expectChips(1200, 980, 820),

                hand("All-in pre-flop, you call and win").holes("AhAs", "9c8c", "KcKd").board("2d7sJh 4c 3h")
                        .script("a c f").expectChips(2020, 980, 0),

                hand("You go all-in on the flop and win").holes("KdKs", "QcJc", "5h5d").board("KhTc2s 8d 3c")
                        .script("c c x | a c f").expectChips(2020, 0, 980),

                hand("You go all-in on the turn and win").holes("JhJd", "Ad3d", "6c6h").board("Jc8s4h Qd 2c")
                        .script("c c x | x x x | a f c").expectChips(2020, 980, 0),

                hand("You go all-in on the river and lose").holes("AcKc", "9h9s", "Th8h").board("9d5c2c Jd 4s")
                        .script("c c x | x x x | x x x | a c f").expectChips(0, 2020, 980),

                hand("Your all-in isn't called").holes("QhQs", "7d2c", "8s3h").board("AdKc9s 5h 4d")
                        .script("c c x | a f f").expectChips(1040, 980, 980),

                hand("You're all-in short, with a side pot").stacks(200, 1000, 1000)
                        .holes("AhAd", "KsKc", "QhQd").board("2c7d9h 4s 3c")
                        .script("r40 a c c | b200 c | x x | x x").expectChips(600, 1000, 600),

                hand("Split pot, the board plays").holes("2h3d", "4c5h", "6d7c").board("AsKsQs Js Ts")
                        .script("c c x | x x x | x x x | x x x").expectChips(1000, 1000, 1000),

                hand("Antes, checked down").ante(5).holes("TcTd", "9s9h", "8c8d").board("AhKh2d 5c 3s")
                        .script("c c x | x x x | x x x | x x x").expectChips(1050, 975, 975)
        );
    }

    private static SampleHand hand(String name)
    {
        return new SampleHand(name).button(2).stacks(1000, 1000, 1000);
    }
}
