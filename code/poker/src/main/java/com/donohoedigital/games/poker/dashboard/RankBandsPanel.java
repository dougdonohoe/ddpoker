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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.gui.*;

import java.awt.*;

/**
 * Rank position graphic for the Player Info dashboard item.  Draws one or two vertical
 * scales, each with a marker showing the band a player's position falls in - the
 * tournament on the left, their own table on the right, or a single scale where the
 * two would say the same thing.  Position 1 is at the top.
 *
 * The same 25x25 footprint as StyleQuadrantsGridPanel, so the two sit alongside each
 * other in the dashboard.  Unlike that one this does not fill its square: DDPanel is
 * not opaque, and only the scales are painted, so the dashboard shows through around
 * them.
 *
 * A scale is divided into one band per player where the field is small enough, and
 * deciles beyond that, so the marker is a band rather than a line - at ten bands in
 * twenty pixels a line would be finer than the thing it points at.  The bands are
 * therefore an approximation; the exact figures are the text beside the graphic.
 */
public class RankBandsPanel extends DDPanel
{
    // Panel geometry - matches StyleQuadrantsGridPanel's 25x25.  A scale is BAR_WIDTH
    // wide with its border drawn a pixel outside that, so the two of them span x=0..10
    // and x=14..24, leaving three clear columns down the middle.  Worth the space: the
    // panel is not opaque, so a one-pixel gap between two dark grey borders reads as a
    // seam rather than as two scales.
    private static final int SIZE = 25;
    private static final int BAR_WIDTH = 9;
    private static final int BAR_HEIGHT = 20;
    private static final int BAR_TOP = 2;
    private static final int LEFT_BAR_X = 1;
    private static final int RIGHT_BAR_X = 15;
    private static final int SINGLE_BAR_X = (SIZE - BAR_WIDTH) / 2;

    /** never divide a scale into more bands than this */
    private static final int MAX_BANDS = 10;

    // palette, taken from StyleQuadrantsGridPanel so this looks native.  Hardcoded as
    // that one and every other painter in the dashboard hardcodes: there is a single
    // shipped styles.xml and no user-selectable colour profile to read.
    private static final Color BACKGROUND = Color.BLACK;
    private static final Color BORDER = Color.DARK_GRAY;
    private static final Color MARKER = new Color(64, 128, 255);

    // current values; rank of 0 means "nothing to show"
    private int nTourneyRank_ = 0;
    private int nTourneyCount_ = 0;
    private int nTableRank_ = 0;
    private int nTableCount_ = 0;

    // when the tournament field IS the table (final table, or a single-table
    // game) both scales would show the same thing, so we draw only one
    private boolean bSingleBar_ = false;

    public RankBandsPanel()
    {
        setPreferredSize(new Dimension(SIZE, SIZE));
    }

    /**
     * Show a player's position.  A rank of 0 draws that scale empty, which is how the
     * Player Info item shows "nothing to report" without the graphic changing shape.
     * bSingleBar collapses to one scale for single-table games, where the two would be
     * identical.
     */
    public void setValues(int nTourneyRank, int nTourneyCount,
                          int nTableRank, int nTableCount, boolean bSingleBar)
    {
        nTourneyRank_ = nTourneyRank;
        nTourneyCount_ = nTourneyCount;
        nTableRank_ = nTableRank;
        nTableCount_ = nTableCount;
        bSingleBar_ = bSingleBar;
        repaint();
    }

    /**
     * Number of bands for a field of the given size.  "Match the count where
     * it's small": one band per player when the field is small enough, deciles
     * otherwise.  A table never has more than 10 seats, so a table scale is
     * always exact.
     */
    private int getNumBands(int nCount)
    {
        if (nCount <= 0) return 0;
        return Math.min(nCount, MAX_BANDS);
    }

    /**
     * Which band (0-based, 0 at the top) a rank falls into.
     */
    private int getBandIndex(int nRank, int nCount, int nBands)
    {
        if (nBands <= 0 || nCount <= 0) return -1;
        int nIndex = (int) (((long) (nRank - 1) * nBands) / nCount);
        if (nIndex < 0) nIndex = 0;
        if (nIndex >= nBands) nIndex = nBands - 1;
        return nIndex;
    }

    /**
     * Top edge of band i, relative to the top of the bar.  Computed as
     * round(i * H / N) so the remainder is spread across the bands - they differ
     * by at most a pixel and none can collapse to zero height.
     */
    private int getBandOffset(int nIndex, int nBands)
    {
        return Math.round((nIndex * (float) BAR_HEIGHT) / nBands);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (bSingleBar_)
        {
            paintScale(g, SINGLE_BAR_X, nTourneyRank_, nTourneyCount_);
        }
        else
        {
            paintScale(g, LEFT_BAR_X, nTourneyRank_, nTourneyCount_);
            paintScale(g, RIGHT_BAR_X, nTableRank_, nTableCount_);
        }
    }

    /**
     * Paint one scale plus its marker.  The scale is painted first and the marker
     * filled over the band the player falls in, so the scale shows above and below it.
     * No per-band divider lines: at two pixels a band there is no room for them.
     */
    private void paintScale(Graphics g, int nX, int nRank, int nCount)
    {
        int nBands = getNumBands(nCount);

        g.setColor(BACKGROUND);
        g.fillRect(nX, BAR_TOP, BAR_WIDTH, BAR_HEIGHT);

        // marker - only when we actually have a player
        if (nRank > 0)
        {
            int nIndex = getBandIndex(nRank, nCount, nBands);
            if (nIndex >= 0)
            {
                int nTop = getBandOffset(nIndex, nBands);
                int nBottom = getBandOffset(nIndex + 1, nBands);
                g.setColor(MARKER);
                g.fillRect(nX, BAR_TOP + nTop, BAR_WIDTH, nBottom - nTop);
            }
        }

        // border last so it is never painted over
        g.setColor(BORDER);
        g.drawRect(nX - 1, BAR_TOP - 1, BAR_WIDTH + 1, BAR_HEIGHT + 1);
    }
}
