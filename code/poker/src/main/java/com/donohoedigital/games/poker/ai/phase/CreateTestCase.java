/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * GameOver.java
 *
 * Created on April 17, 2003, 9:20 PM
 */

package com.donohoedigital.games.poker.ai.phase;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class CreateTestCase extends DialogPhase
{
    static Logger logger = LogManager.getLogger(CreateTestCase.class);

    private PokerGame game_;
    private PokerTable table_;
    private HoldemHand hhand_;
    private PokerPlayer player_;
    private ListPanel outcomeList_;

    /**
     * init phase
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        game_ = (PokerGame) context.getGame();
        table_ = game_.getCurrentTable();
        hhand_ = table_.getHoldemHand();
        player_ = hhand_.getCurrentPlayer();

        super.init(engine, context, gamephase);
    }

    /**
     * end game
     */
    public JComponent createDialogContents()
    {
        DDPanel base = new DDPanel(GuiManager.DEFAULT, STYLE);
        outcomeList_ = new ListPanel(OutcomeItemPanel.class, STYLE);
        outcomeList_.setSelectedIcon(ImageConfig.getImageIcon("pokericon16png"));
        RuleEngine ruleEngine = ((V2Player)player_.getPokerAI()).getRuleEngine();
        outcomeList_.setItems(ruleEngine.getEligibleOutcomeNames());
        outcomeList_.setSelectedItem(ruleEngine.getStrongestOutcomeName());
        base.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        base.setLayout(new BorderLayout(16, 16));
        base.add(outcomeList_, BorderLayout.CENTER);
        base.setPreferredSize(new Dimension(200, 200));

        this.getMatchingButton("results").addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        AITest.test(context_);
                    }
                }
        );

        return base;
    }


    public static class OutcomeItemPanel extends ListItemPanel
    {
        private DDLabel label_;

        public OutcomeItemPanel(ListPanel panel, Object item, String sStyle)
        {
            super(panel, item, sStyle);

            label_ = new DDLabel(GuiManager.DEFAULT, sStyle);

            add(label_, BorderLayout.CENTER);
        }

        public void update()
        {
            label_.setText(RuleEngine.getOutcomeLabel((String)item_));
        }

        public void setIcon(ImageIcon icon)
        {
            label_.setIcon(icon);
        }
    }

    public void finish() {

        GameButton button = (GameButton)getResult();

        if (button.getName().equals("okay"))
        {
            File fDir = AITest.getTestCaseDir();

            GameState newSave = GameStateFactory.createGameState(null, fDir, "testcase", "ddpokersave", null);

            // prevent issues during online updates from other clients
            synchronized (game_.isOnlineGame() ? context_.getGameManager().getSaveLockObject() : this)
            {
                game_.saveGame(newSave);
                game_.writeGame(newSave);
            }

            try
            {
                FileWriter fw = new FileWriter(newSave.getFile().getAbsolutePath() + ".expect");
                fw.write((String)outcomeList_.getSelectedItem());
                fw.write("\n");
                fw.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        super.finish();
    }
}
