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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;

import java.awt.*;

public class HandProbabilityMatrixPanel extends DDPanel
{
    private HandProbabilityMatrix matrix_;

    private DDPanel panels_[][] = new DDPanel[52][52];

    private static int gridline_ = 1;
    private static int subgridline_ = 1;

    public HandProbabilityMatrixPanel()
    {
        setLayout(new GridLayout(13, 13, gridline_, gridline_));
        setOpaque(true);
        setBackground(Color.LIGHT_GRAY);

        for (int yy = 0; yy < 13; ++yy)
        {
            for (int xx = 0; xx < 13; ++xx)
            {
                DDPanel subpanel = new DDPanel();
                subpanel.setOpaque(true);
                subpanel.setBackground(Color.BLACK);
                //subpanel.setBorder(BorderFactory.createEmptyBorder(subgridline_, subgridline_, subgridline_, subgridline_));

                if (xx >= yy)
                {
                    subpanel.setLayout(new GridLayout(4, 4, subgridline_, subgridline_));

                    for (int y = yy*4; y < yy*4+4; ++y)
                    {
                        for (int x = xx*4; x < xx*4+4; ++x)
                        {
                            if (x > y)
                            {
                                panels_[x][y] = new DDPanel();
                                panels_[x][y].setPreferredSize(new Dimension(8,8));
                                subpanel.add(panels_[x][y]);
                            }
                            else
                            {
                                subpanel.add(new DDPanel());
                            }
                        }
                    }
                }
                add(subpanel);
            }
        }

        //setPreferredSize(new Dimension(312,312));
    }

    public void setHandProbabilityMatrix(HandProbabilityMatrix matrix)
    {
        matrix_ = matrix;

        updateColors();
    }

    public void updateColors(float[][] values)
    {
        for (int y = 0; y < 52; ++y)
        {
            for (int x = y + 1; x < 52; ++x)
            {
                float value = values[51-x][51-y];
                if (value != 0)
                {
                    panels_[x][y].setOpaque(true);
                    if (value > 0)
                    {
                        panels_[x][y].setBackground(new Color((int) (255.0 * value), 0, 0));
                    }
                    else
                    {
                        panels_[x][y].setBackground(new Color(0, (int) (-255.0 * value), 0));
                    }
                }
                else
                {
                    panels_[x][y].setOpaque(false);
                }
                //panels_[x][y].setBackground(Color.RED);
            }
        }
        repaint();
    }

    public void updateColors()
    {
        for (int y = 0; y < 52; ++y)
        {
            for (int x = y+1; x < 52; ++x)
            {
                Card card1 = Card.getCard(x % 4, 14 - x / 4);
                Card card2 = Card.getCard(y % 4, 14 - y / 4);
                float prob = matrix_.getProbability(card1, card2);
                //float odds = matrix_.getOdds(card1, card2);
                //float value = prob * odds;
                float value = prob;
                if (value != 0)
                {
                    panels_[x][y].setOpaque(true);
                    if (value > 0)
                    {
                        panels_[x][y].setBackground(new Color((int)(255.0 * value), 0, 0));
                    }
                    else
                    {
                        panels_[x][y].setBackground(new Color(0, (int) (-255.0 * value), 0));
                    }
                }
                else
                {
                    panels_[x][y].setOpaque(false);
                }
                //panels_[x][y].setBackground(Color.RED);
            }
        }
        repaint();
    }
}
