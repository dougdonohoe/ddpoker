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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.math.*;
import java.util.*;
import java.util.List;

public class PokerShowdownPanel extends DDTabPanel implements DDProgressFeedback, ChangeListener
{
    static Logger logger = LogManager.getLogger(PokerShowdownPanel.class);

    private static Dimension resultsSize = new Dimension(90, 50);

    private GameContext context_;
    private PokerTable table_;
    private DDProgressBar progress_;
    private String STYLE;
    private SimulatorDialog sim_;
    private OptionInteger numOpponents_, numSims_;
    private DDRadioButton allcombo_, simcombo_;
    private List<DDLabelBorder> opponents_ = new ArrayList<DDLabelBorder>();
    private boolean bStopRequested_ = false;
    private boolean bIterWayBig_;
    private GlassButton run_, stop_;
    private boolean bDemo_;

    /**
     * Create showdown panel
     */
    public PokerShowdownPanel(GameContext context, SimulatorDialog sim, PokerTable table, String sStyle)
    {
        super();
        context_ = context;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
        sim_ = sim;
        table_ = table;
        STYLE = sStyle;
        setPreferredSize(new Dimension(420, 355));
    }

    /**
     * create UI upon demand
     */
    @Override
    protected void createUI()
    {
        setBorderLayoutGap(5, 0);

        // top
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(5, 0);
        add(top, BorderLayout.NORTH);

        // html
        DDPanel toptop = new DDPanel();
        toptop.setBorderLayoutGap(7, 0);
        top.add(toptop, BorderLayout.NORTH);

        DDHtmlArea test = new DDHtmlArea(GuiManager.DEFAULT, "PokerStatsHeader");
        toptop.add(test, BorderLayout.NORTH);
        test.setBorder(BorderFactory.createEmptyBorder());
        test.setText(PropertyConfig.getMessage("msg.sim.showdown"));

        // control parts
        DDPanel controlbase = new DDPanel();
        toptop.add(controlbase, BorderLayout.CENTER);
        controlbase.setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.LEFT, 5, 0,
                                                       HorizontalFlowLayout.CENTER));

        TypedHashMap dummy = new TypedHashMap();
        numOpponents_ = new OptionInteger(null, "numopp", STYLE, dummy, null, 1, 9, -1, true);
        numOpponents_.addChangeListener(this);
        numOpponents_.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (numOpponents_.getSpinner().isValidData())
                {
                    updateNumOpponents();
                }
            }
        });
        controlbase.add(numOpponents_);

        DDPanel radios = new DDPanel();
        radios.setLayout(new GridLayout(2, 1, 0, -3));
        controlbase.add(radios);

        DDPanel simbase = new DDPanel();
        ButtonGroup group = new ButtonGroup();
        radios.add(simbase);
        simcombo_ = new DDRadioButton("simulate", STYLE);
        simcombo_.setSelected(true);
        group.add(simcombo_);
        simbase.add(simcombo_, BorderLayout.WEST);
        // listener to control # sims
        ActionListener comboListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                numSims_.setEnabled(simcombo_.isSelected());
            }
        };

        allcombo_ = new DDRadioButton("allcombos", STYLE);
        group.add(allcombo_);
        radios.add(allcombo_);

        simcombo_.addActionListener(comboListener);
        allcombo_.addActionListener(comboListener);

        int nMax = 10000000;
        bDemo_ = GameEngine.getGameEngine().isDemo();
        numSims_ = new OptionInteger(null, "hands", STYLE, dummy, bDemo_ ? 10000 : null,
                                     bDemo_ ? 1000 : 25000,
                                     bDemo_ ? 10000 : nMax, -1, true);
        numSims_.getSpinner().setValue(bDemo_ ? 10000 : nMax);
        if (!bDemo_)
        {
            numSims_.getSpinner().setUseBigStep(true);
            numSims_.getSpinner().resetPreferredSize();
            numSims_.resetToDefault();
        }
        else
        {
            numSims_.getSpinner().setStep(1000);
        }
        numSims_.setEnabled(true);
        numSims_.addChangeListener(this);
        simbase.add(numSims_, BorderLayout.CENTER);

        // progress bar
        progress_ = new DDProgressBar(GuiManager.DEFAULT, "PokerStats", false);
        progress_.setProgressFeedback(this);

        DDPanel pb = new DDPanel();
        pb.setBorderLayoutGap(0, 5);

        run_ = new GlassButton("run", "Glass");
        run_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sim_.bSimRunning_ = true;
                bStopRequested_ = false;
                clearResults();
                run_.setEnabled(false);
                numSims_.setEnabled(false);
                numOpponents_.setEnabled(false);
                allcombo_.setEnabled(false);
                simcombo_.setEnabled(false);
                stop_.setEnabled(true);
                if (simcombo_.isSelected())
                {
                    if (bDemo_)
                    {
                        EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.showdown.demo"));
                    }
                    runSimulator();
                }
                else
                {
                    runIterator();
                }
            }
        });
        stop_ = new GlassButton("stop", "Glass");
        stop_.setEnabled(false);
        stop_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setStopRequested();
            }
        });

        pb.add(run_, BorderLayout.WEST);
        pb.add(progress_, BorderLayout.CENTER);
        pb.add(stop_, BorderLayout.EAST);

        top.add(pb, BorderLayout.CENTER);

        // opponent grid
        DDPanel opponents = new DDPanel();
        add(GuiUtils.CENTER(opponents), BorderLayout.CENTER);
        opponents.setLayout(new GridLayout(2, 5, 10, 10));
        SimulatorDialog.SimHandPanel cards;

        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            cards = new SimulatorDialog.SimHandPanel(sim_, table_, table_.getPlayer(i).getHand());
            DDLabelBorder boardcards = new ShowBorder(i == 0 ? "myhand" : "opponent", cards, i);
            opponents.add(boardcards);
            opponents_.add(boardcards);
        }

        // update enabled
        updateNumOpponents();
    }

    /**
     * num opponents or num sims changed
     */
    public void stateChanged(ChangeEvent e)
    {
        if (!stop_.isEnabled())
        {
            run_.setEnabled(numSims_.getSpinner().isValidData() &&
                            numOpponents_.getSpinner().isValidData());
        }
    }

    /**
     * labelborder for each hand
     */
    private class ShowBorder extends DDLabelBorder
    {
        SimulatorDialog.SimHandPanel cards;
        DDHtmlArea results;

        ShowBorder(String sName, SimulatorDialog.SimHandPanel cards, int i)
        {
            super(sName, STYLE);
            this.cards = cards;
            setBorderLayoutGap(5, 0);
            if (i > 0) setText(PropertyConfig.formatMessage(getText(), i));
            add(GuiUtils.CENTER(cards), BorderLayout.NORTH);

            results = new DDHtmlArea(GuiManager.DEFAULT, "PokerStats");
            results.setPreferredSize(resultsSize);
            results.setBorder(BorderFactory.createEmptyBorder());
            add(results, BorderLayout.CENTER);
        }

        @Override
        public void setEnabled(boolean b)
        {
            super.setEnabled(b);
            cards.setEnabled(b);
        }

        public void setResults(String s)
        {
            results.setText(s);
        }
    }

    /**
     * set num players
     */
    public void setNumOpponents(int nNum)
    {
        numOpponents_.getSpinner().setValue(nNum);
    }

    /**
     * update number of opponents by enabling cards
     */
    private void updateNumOpponents()
    {
        int nNum = numOpponents_.getSpinner().getValue();
        ShowBorder show;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            show = (ShowBorder) opponents_.get(i);
            show.setEnabled(i <= nNum);
        }
        updateDisplay(true);
    }

    /**
     * update display - number of combos and results clearing if requested
     */
    public void updateDisplay(boolean bClearResults)
    {
        HoldemHand hhand = sim_.hhand_;
        PokerTable table = sim_.table_;

        Hand[] hands = new Hand[numOpponents_.getSpinner().getValue() + 1];
        for (int i = 0; i < hands.length; i++)
        {
            hands[i] = table.getPlayer(i).getHand();
        }

        Hand community = hhand.getCommunity();
        BigInteger num = HoldemSimulator.getNumberIterations(hands, community);
        String sValue = num.toString();
        bIterWayBig_ = (sValue.length() > 7);
        long nNum = Long.MAX_VALUE;
        if (!bIterWayBig_)
        {
            nNum = num.longValue();
        }
        StringBuilder commas = new StringBuilder();
        int nCnt = 0;
        for (int i = sValue.length() - 1; i >= 0; i--)
        {
            if (nCnt > 0 && nCnt % 3 == 0) commas.append(",");
            commas.append(sValue.charAt(i));
            nCnt++;
        }
        commas = commas.reverse();

        if (bClearResults) clearResults();
        allcombo_.setText(PropertyConfig.getMessage("msg.allcombos", commas));

        // auto select all combo if it is 2,000,000 or less
        if (nNum <= 2000000)
        {
            allcombo_.setSelected(true);
        }
        else
        {
            simcombo_.setSelected(true);
        }
        numSims_.setEnabled(simcombo_.isSelected());
    }

    /**
     * clear results in all hands
     */
    private void clearResults()
    {
        progress_.setPercentDone(0);
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            setResults(i, "");
        }
    }

    /**
     * set results for hand i
     */
    private void setResults(int i, String s)
    {
        ShowBorder show = (ShowBorder) opponents_.get(i);
        show.setResults(s);
    }

    /**
     * run simulator
     */
    private void runSimulator()
    {
        new UpdateThread(true).start();
    }

    /**
     * run iter, warn if too many results
     */
    private void runIterator()
    {
        if (bIterWayBig_ &&
            !EngineUtils.displayConfirmationDialog(context_, PropertyConfig.getMessage("msg.bigiter"), "bigiter"))
        {
            setFinalResult(null);
            return;
        }
        new UpdateThread(false).start();
    }

    /**
     * stop requested on simulator panel
     */
    public boolean isStopRequested()
    {
        return bStopRequested_;
    }

    /**
     * set stop requested
     */
    void setStopRequested()
    {
        bStopRequested_ = true;
    }

    /**
     * progres bar handles this
     */
    public void setMessage(String sMessage)
    {
    }

    /**
     * progress bar handles this
     */
    public void setPercentDone(int n)
    {
    }

    /**
     * progress bar passes this onto us when done
     */
    public void setFinalResult(Object oResult)
    {
        sim_.bSimRunning_ = false;
        stop_.setEnabled(false);
        run_.setEnabled(true);
        allcombo_.setEnabled(true);
        simcombo_.setEnabled(true);
        numSims_.setEnabled(simcombo_.isSelected());
        numOpponents_.setEnabled(true);

        setIntermediateResult(oResult);
    }

    /**
     * set results for what we have so far
     */
    public void setIntermediateResult(Object oResult)
    {
        if (oResult == null) return;

        final StatResult[] stats = (StatResult[]) oResult;

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                StatResult stat;
                for (int i = 0; i < stats.length; i++)
                {
                    stat = stats[i];
                    if (stat == null) continue;
                    setResults(i, stat.toHTML("msg.showdown.results"));
                }
            }
        });
    }

    /**
     * thread to run simulator
     */
    private class UpdateThread extends Thread
    {
        private boolean bSimulate;

        public UpdateThread(boolean bSim)
        {
            super("UpdateThread");
            bSimulate = bSim;
        }

        @Override
        public void run()
        {
            if (false && TESTING(EngineConstants.TESTING_PERFORMANCE)) Perf.start();

            HoldemHand hhand = sim_.hhand_;
            PokerTable table = sim_.table_;

            Hand[] hands = new Hand[numOpponents_.getSpinner().getValue() + 1];
            for (int i = 0; i < hands.length; i++)
            {
                hands[i] = new Hand(table.getPlayer(i).getHand());
                if (bSimulate) hands[i].removeBlank();
                else replaceBlank(hands[i]);
            }

            Hand community = new Hand(hhand.getCommunity());
            if (bSimulate) community.removeBlank();
            else replaceBlank(community);

            if (bSimulate)
            {
                HoldemSimulator.simulate(hands, community, numSims_.getSpinner().getValue(), progress_);
            }
            else
            {
                HoldemSimulator.iterate(hands, community, progress_);
            }

            if (false && TESTING(EngineConstants.TESTING_PERFORMANCE)) Perf.stop();
        }
    }

    /**
     * replace any blank cards with new blank cards so they
     * can be modified w/out changing display
     */
    private void replaceBlank(Hand hand)
    {
        Card c;
        for (int i = hand.size() - 1; i >= 0; i--)
        {
            if (hand.getCard(i).isBlank())
            {
                c = new Card();
                c.setValue(Card.BLANK);
                hand.setCard(i, c);
            }
        }
    }
}
