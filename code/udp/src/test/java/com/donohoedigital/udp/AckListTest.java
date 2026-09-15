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
package com.donohoedigital.udp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AckList keeps received message ids as a sorted list of contiguous ranges, merging
 * ranges as gaps fill in.  Acks arrive in any order and may repeat.
 */
public class AckListTest
{
    @Test
    public void emptyList()
    {
        AckList list = new AckList(1);
        assertEquals(0, list.size());
        assertEquals("[empty]", list.toString());
    }

    @Test
    public void gapsStaySeparateUntilFilled()
    {
        AckList list = ack(1, 2, 3, 7, 8, 5);
        assertEquals("[1...3], [5], [7...8]", list.toString());

        list.ack(4);
        assertEquals("[1...5], [7...8]", list.toString());

        list.ack(6);
        assertEquals("[1...8]", list.toString());
    }

    @Test
    public void prependsBeforeFirstRange()
    {
        AckList list = ack(10, 5, 1);
        assertEquals("[1], [5], [10]", list.toString());

        list.ack(9);
        list.ack(0);
        assertEquals("[0...1], [5], [9...10]", list.toString());
    }

    @Test
    public void containsThroughRequiresNoGapsFromOne()
    {
        assertTrue(new AckList(1).containsThrough(0), "nothing to contain");
        assertFalse(new AckList(1).containsThrough(1), "empty");

        AckList list = ack(1, 2, 3, 5);
        assertTrue(list.containsThrough(3));
        assertFalse(list.containsThrough(4));
        assertFalse(list.containsThrough(5));

        list.ack(4);
        assertTrue(list.containsThrough(5));
        assertFalse(ack(2, 3).containsThrough(3), "missing 1");
    }

    @Test
    public void duplicatesAreIgnored()
    {
        AckList list = ack(5, 5, 6, 4, 6, 5, 4);
        assertEquals("[4...6]", list.toString());
    }

    /**
     * Hand-picked sequence from the old AckList.ackTest() - out of order, with repeats
     */
    @Test
    public void scrambledSequenceWithRepeats()
    {
        AckList list = ack(200, 201, 202, 205, 204, 203, 197, 195, 196, 198, 199, 180, 185, 190, 192,
                           193, 195, 194, 183, 196, 197, 201, 184, 182, 189, 186, 188, 187, 181, 191, 182);
        assertEquals("[180...205]", list.toString());
    }

    @Test
    public void shuffledAcksCollapseToOneRange()
    {
        List<Integer> ids = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) ids.add(i);
        Collections.shuffle(ids, new Random(42));

        AckList list = new AckList(1);
        for (int id : ids) list.ack(id);

        assertEquals(1, list.size());
        assertEquals("[1...1000]", list.toString());
    }

    /**
     * Random acks with repeats and misses - every acked id is contained, every missed id
     * is not, and there is one range per run of consecutive acked ids.
     */
    @Test
    public void randomAcksMatchWhatWasAcked()
    {
        int size = 10000;
        boolean[] acked = new boolean[size + 1];
        Random random = new Random(42);

        AckList list = new AckList(1);
        for (int i = 0; i < size * 7; i++)
        {
            int id = random.nextInt(size) + 1;
            acked[id] = true;
            list.ack(id);
        }

        // sanity: 7 passes should still leave some gaps
        int missed = 0;
        int runs = 0;
        for (int id = 1; id <= size; id++)
        {
            assertEquals(acked[id], list.contains(data(id)), "id " + id);
            if (!acked[id]) missed++;
            else if (!acked[id - 1]) runs++;
        }
        assertTrue(missed > 0, "expected some missed ids");
        assertEquals(runs, list.size());
    }

    private static AckList ack(int... ids)
    {
        AckList list = new AckList(1);
        for (int id : ids) list.ack(id);
        return list;
    }

    private static UDPData data(int id)
    {
        return new UDPData(UDPData.Type.MESSAGE, id, (short) 1, (short) 1, null, 0, 0, UDPData.USER_TYPE_UNSPECIFIED);
    }
}
