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
 * scales, each divided into bands, with a marker line showing where a player
 * sits.  Deliberately the same 25x25 footprint as StyleQuadrantsGridPanel so the
 * two read as siblings in the dashboard.
 *
 * The bar is a fixed scale and the marker travels along it - the marker is drawn
 * over the scale and does not repaint the rest of it.  Position 1 is at the top.
 *
 * The bands are an approximation; exact figures are shown as text below the
 * graphic by the Player Info item.
 */
public class RankBandsPanel extends DDPanel
{
    // panel geometry - matches StyleQuadrantsGridPanel's 25x25
    private static final int SIZE = 25;
    private static final int BAR_WIDTH = 9;
    private static final int BAR_HEIGHT = 20;
    private static final int BAR_TOP = 2;
    private static final int LEFT_BAR_X = 2;
    private static final int RIGHT_BAR_X = 14;
    private static final int SINGLE_BAR_X = (SIZE - BAR_WIDTH) / 2;

    /** never divide a scale into more bands than this */
    private static final int MAX_BANDS = 10;

    // default palette, taken from StyleQuadrantsGridPanel so this looks native
    static final Color DEFAULT_BACKGROUND = Color.BLACK;
    static final Color DEFAULT_BORDER = Color.DARK_GRAY;
    static final Color DEFAULT_MARKER = new Color(64, 128, 255);

    // palette - settable so the user can pick an alternative (e.g. green->red)
    private Color colBackground_ = DEFAULT_BACKGROUND;
    private Color colBorder_ = DEFAULT_BORDER;
    private Color colMarker_ = DEFAULT_MARKER;

    // when both are non-null the scale is painted as a banded gradient between
    // them (top colour -> bottom colour) instead of a flat background
    private Color colScaleTop_ = null;
    private Color colScaleBottom_ = null;

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
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(SIZE, SIZE));
    }

    /**
     * Set the palette.  Pass null for scaleTop/scaleBottom to paint a flat
     * background instead of a gradient.
     */
    public void setPalette(Color background, Color border, Color marker,
                           Color scaleTop, Color scaleBottom)
    {
        colBackground_ = background;
        colBorder_ = border;
        colMarker_ = marker;
        colScaleTop_ = scaleTop;
        colScaleBottom_ = scaleBottom;
        repaint();
    }

    /**
     * Show a player's position.  bSingleBar collapses to one scale for the final
     * table and single-table games, where the two would be identical.
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
     * Nothing hovered / nothing to show - scales are drawn empty.
     */
    public void clear()
    {
        nTourneyRank_ = 0;
        nTourneyCount_ = 0;
        nTableRank_ = 0;
        nTableCount_ = 0;
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
    public void paintComponent(Graphics g1)
    {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;

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
     * Paint one scale plus its marker.  The scale is painted first and the
     * marker drawn over it, so the rest of the scale shows through either side.
     */
    private void paintScale(Graphics2D g, int nX, int nRank, int nCount)
    {
        int nBands = getNumBands(nCount);

        // background / scale.  No per-band divider lines: at ~2px a band there
        // is no room for them.  With a gradient the bands read as colour steps.
        if (nBands > 0 && colScaleTop_ != null && colScaleBottom_ != null)
        {
            for (int i = 0; i < nBands; i++)
            {
                int nTop = getBandOffset(i, nBands);
                int nBottom = getBandOffset(i + 1, nBands);
                g.setColor(blend(colScaleTop_, colScaleBottom_, i, nBands));
                g.fillRect(nX, BAR_TOP + nTop, BAR_WIDTH, nBottom - nTop);
            }
        }
        else
        {
            g.setColor(colBackground_);
            g.fillRect(nX, BAR_TOP, BAR_WIDTH, BAR_HEIGHT);
        }

        // marker - only when we actually have a player
        int nIndex = getBandIndex(nRank, nCount, nBands);
        if (nRank > 0 && nIndex >= 0)
        {
            int nTop = getBandOffset(nIndex, nBands);
            int nBottom = getBandOffset(nIndex + 1, nBands);
            g.setColor(colMarker_);
            g.fillRect(nX, BAR_TOP + nTop, BAR_WIDTH, nBottom - nTop);
        }

        // border last so it is never painted over
        g.setColor(colBorder_);
        g.drawRect(nX - 1, BAR_TOP - 1, BAR_WIDTH + 1, BAR_HEIGHT + 1);
    }

    /**
     * Colour for band i of n, interpolated between the two scale endpoints.
     * Banded rather than smooth so the band structure stays visible.
     */
    private Color blend(Color top, Color bottom, int nIndex, int nBands)
    {
        float f = (nBands <= 1) ? 0f : (nIndex / (float) (nBands - 1));
        int r = Math.round(top.getRed() + f * (bottom.getRed() - top.getRed()));
        int g = Math.round(top.getGreen() + f * (bottom.getGreen() - top.getGreen()));
        int b = Math.round(top.getBlue() + f * (bottom.getBlue() - top.getBlue()));
        return new Color(r, g, b);
    }
}
