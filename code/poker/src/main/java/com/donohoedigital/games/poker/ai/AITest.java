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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class AITest
{
    private static ResultsDialog dialog_ = null;

    public static File getTestCaseDir()
    {
        File fDir = new File(GameConfigUtils.getSaveDir(), "aitest");

        if (!fDir.exists())
        {
            fDir.mkdirs();
        }

        return fDir;
    }

    public static void test(GameContext context)

    {
        test(context, PlayerType.getDefaultProfile());
    }

    public static void test(GameContext context, PlayerType playerType)
    {
        if (dialog_ == null)
        {
            dialog_ = new ResultsDialog(context, playerType, "Test Results");
        }
        dialog_.setVisible(true);
        dialog_.refresh();
    }

    private static class ResultsDialog extends JDialog
    {
        DDHtmlArea htmlArea;
        PlayerType playerType_;
        GameContext context_;

        public ResultsDialog(GameContext context, PlayerType playerType, String title)
        {
            super(context.getFrame(), title, false);

            context_ = context;
            playerType_ = playerType;

            getContentPane().setLayout(new BorderLayout());

            htmlArea = new DDHtmlArea();

            htmlArea.addHyperlinkListener(new HyperlinkListener()
            {
                public void hyperlinkUpdate(HyperlinkEvent e)
                {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                    {
                        File file = new File(e.getDescription());
                        GameState gameState = GameStateFactory.createGameState(file, false);

                        //PokerGame game = (PokerGame)engine.createGame(gameState);
                        context_.setGameManager(null);
                        LoadSavedGame.loadGame(context_, gameState);
                        PokerGame game = (PokerGame)context_.getGame();
                        /*
                        if (game == null)
                        {
                            LoadSavedGame.loadGame(engine, gameState);
                            game = (PokerGame)engine.getGame();
                        }
                        else
                        {
                            game.loadGame(gameState, true);
                        }
                        */
                        PokerTable table = game.getCurrentTable();
                        HoldemHand hhand = table.getHoldemHand();
                        PokerPlayer player = hhand.getCurrentPlayer();
                        player.setPlayerType(playerType_);
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(htmlArea);

            DDPanel buttons = new DDPanel();
            buttons.setLayout(new FlowLayout());

            GlassButton refreshButton = new GlassButton(GuiManager.DEFAULT,  "BrushedMetal");
            refreshButton.setText("Refresh");
            refreshButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    refresh();
                }
            });
            buttons.add(refreshButton);

            getContentPane().add(scroll, BorderLayout.CENTER);
            getContentPane().add(buttons, BorderLayout.NORTH);

            this.setSize(400, 768);

            refresh();
        }

        private String describeTestCase(PokerPlayer player)
        {
            StringBuilder buf = new StringBuilder();

            HoldemHand hhand = player.getHoldemHand();

            int round = hhand.getRound();

            int numPlayers = hhand.getTable().getNumOccupiedSeats();

            buf.append(player.getHand().toHTML());
            buf.append("</td><td>");
            buf.append(HoldemHand.getRoundName(round));
            buf.append(" - ");
            buf.append(numPlayers);
            buf.append(" players ");
            buf.append("<br>");
            buf.append(PokerPlayer.getPositionName(player.getPositionCategory()));
            buf.append(" position - ");
            switch (player.getHoldemHand().getPotStatus())
            {
                case PokerConstants.NO_POT_ACTION:
                    buf.append("first to act ");
                    break;
                case PokerConstants.CALLED_POT:
                    buf.append("called pot ");
                    break;
                case PokerConstants.RAISED_POT:
                    buf.append("raised pot ");
                    break;
                case PokerConstants.RERAISED_POT:
                    buf.append("reraised pot ");
                    break;
            }

            return buf.toString();
        }

        private void refresh()
        {
            StringBuilder buf = new StringBuilder();
            StringBuilder okbuf = new StringBuilder();
            StringBuilder errbuf = new StringBuilder();

            File fDir = getTestCaseDir();

            File files[] = fDir.listFiles(new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return (name.endsWith(".ddpokersave"));
                }
            });

            Arrays.sort(files);

            for (int i = 0; i < files.length; ++i)
            {
                try
                {
                    runTest(playerType_, errbuf, okbuf, files[i]);
                }
                catch (IOException e)
                {
                    buf.append("IO Exception processing ");
                    buf.append(files[i].getName());
                    buf.append("<pre><tt>");
                    buf.append(e.toString());
                    buf.append("</tt></pre>");
                    buf.append("<br>");
                }
            }

            buf.append("<table><tr><td colspan=\"3\"><b>Test Case</b></td><td><b>Expected</b></td><td><b>Actual</b></td></tr>");
            if (errbuf.length() > 0)
            {
                buf.append(errbuf);
                buf.append("<tr><td colspan=\"5\"><hr></td></tr>");
            }
            buf.append(okbuf);
            buf.append("</table>");

            htmlArea.setText(buf.toString());
        }

        public void runTest(PlayerType playerType, StringBuilder errbuf, StringBuilder okbuf, File file) throws IOException
        {
            GameState gameState = GameStateFactory.createGameState(file, false);
            //LoadSavedGame.loadGame(GameEngine.getGameEngine(), gameState);
            context_.createGame(gameState, false);
            PokerGame game = (PokerGame)context_.getGame();
            PokerTable table = game.getCurrentTable();
            HoldemHand hhand = table.getHoldemHand();
            PokerPlayer player = hhand.getCurrentPlayer();
            player.setPlayerType(playerType);
            PokerAI pokerAI = player.getPokerAI();
            RuleEngine ruleEngine = ((V2Player) pokerAI).getRuleEngine();
            pokerAI.getHandAction(false);
            String actual = (ruleEngine.getStrongestOutcomeName());
            String expected = new BufferedReader(new FileReader(new File(file.getAbsolutePath() + ".expect"))).readLine();
            boolean ok = (actual.equals(expected));
            StringBuilder buf = ok ? okbuf : errbuf;
            buf.append("<tr><td><a href=\"");
            buf.append(file.getAbsolutePath());
            buf.append("\">");
            buf.append(Integer.parseInt(file.getName().substring(9, 15)));
            buf.append("</a></td><td>");
            buf.append(describeTestCase(player));
            buf.append("</td><td>");
            buf.append(expected);
            buf.append("</td><td>");
            if (!ok)
            {
                buf.append("<font color=\"#FF0000\">");
            }
            else
            {
                buf.append("<font color=\"#009900\">");
            }
            buf.append(actual);
            buf.append("</font>");
            //buf.append("<br>");
            //buf.append(ruleEngine.toHTML());
            buf.append("</td></tr>");
        }
    }
}
