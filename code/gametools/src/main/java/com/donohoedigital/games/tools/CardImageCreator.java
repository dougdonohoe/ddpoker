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
/*
 * CardImageCreator.java
 *
 * Created on October 13, 2005, 8:55 AM 
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

/**
 *
 * @author  Doug Donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class CardImageCreator extends BaseCommandLineApp
{
    // logging
    private static Logger logger = LogManager.getLogger(CardImageCreator.class);

    /**
     * Run emailer
     */
    public static void main(String[] args) {
        try {
            Prefs.setRootNodeName("poker2");
            new CardImageCreator("poker", args);
        }

        catch (ApplicationError ae)
        {
            System.err.println("CardImageCreator ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }  
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
        catch (Throwable t)
        {
            System.err.println("CardImageCreator ending due to ApplicationError: " + Utils.formatExceptionText(t));
            System.exit(1);
        }
    }

    /**
     * Create War from config file
     */
    public CardImageCreator(String sConfigName, String[] args)
    {
        super(sConfigName, args);
        ConfigManager.getConfigManager().loadGuiConfig();

        // write cards
        writeCards();
    }

    /**
     * write cards
     */
    private void writeCards()
    {
        CardThumbnail piece = new OutputCard();
        Rectangle rect = new Rectangle(20, 26);
        int border = 1;

        Deck deck = new Deck(false);
        deck.add(Card.BLANK);

        Card card;
        File file;
        Graphics2D g;
        BufferedImage image;
        String sName;
        for (int i = 0; i < 53; i++)
        {
            card = deck.getCard(i);
            sName = card.isBlank() ? "blank":card.getRankDisplaySingle() + card.getSuitDisplay();
            file = new File(".", "card_"+sName+".png");
            logger.debug("Processing " + card +" to "+ file.getName());
            image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
            piece.setCard(card);
            piece.setUp(true);
            piece.setStroke(true);

            g = (Graphics2D) image.getGraphics();
            piece.drawImageAt(g, piece.getImageComponent(), 0, 0, 0,
                rect.x+border, rect.y+border, rect.width-border*2,  rect.height-border*2,
                1.0d);
            try {
                ImageIO.write(image, "png", file);
            }
            catch (IOException ioe)
            {
                logger.error(card+": "+Utils.formatExceptionText(ioe));
            }
        }
    }

    /**
     * card for writing
     */
    private class OutputCard extends CardThumbnail
    {
        protected boolean isLargePref()
        {
            return false;
        }

        protected boolean is4ColorPref()
        {
            return true;
        }

        protected String getDeckPref()
        {
            return "n/a";
        }
    }
}
