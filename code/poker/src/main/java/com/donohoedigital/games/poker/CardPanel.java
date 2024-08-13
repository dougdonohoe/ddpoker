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

import com.donohoedigital.gui.*;
import com.donohoedigital.games.poker.engine.*;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 22, 2005
 * Time: 1:19:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class CardPanel extends DDPanel
{
    protected CardPiece cp_;

    public CardPanel(Card c, boolean bThumb)
    {
        cp_ = new CardPiece(null, null, null, true, 0);
        cp_.setCard(c);
        cp_.setShadow(false);
        cp_.setThumbnailMode(bThumb);
    }

    public CardPiece getCardPiece()
    {
        return cp_;
    }

    public CardPanel(CardPiece cp)
    {
        cp_ = cp;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (cp_ != null)
        {
            cp_.setEnabled(isEnabled());
            cp_.drawImageAt((Graphics2D) g, cp_.ic_, 0, 0, 0,
                    0, 0, getWidth(), getHeight(), 1.0d);
        }
    }

}
