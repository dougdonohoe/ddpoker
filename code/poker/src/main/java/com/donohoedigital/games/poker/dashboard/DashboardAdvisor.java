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

import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DashboardAdvisor extends DashboardItem
{
    JScrollPane scroll_;
    DDHtmlArea htmlAdvice_;
    String title_;
    String advice_;
    String verboseAdvice_;
    PokerTable table_;
    int playerID_;

    boolean bVerbose_ = TESTING(PokerConstants.TESTING_ADVISOR_VERBOSE);
    public static final String NOADVICE = PropertyConfig.getMessage("msg.advisor.noadvice");
    public static final String NOADVICETITLE = PropertyConfig.getMessage("msg.advisor.noadvice.title");

    private DDPanel buttons_;

    public DashboardAdvisor(GameContext context)
    {
        super(context, "advisor");
        setDynamicTitle(true);
        //setTableEventsImmediate(); // we need them immediately to prevent race conditions
        trackTableEvents(PokerTableEvent.TYPE_NEW_HAND |
                         PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED |
                         PokerTableEvent.TYPE_CARD_CHANGED |
                         PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED | // could change pot odds, for example
                         PokerTableEvent.TYPE_BUTTON_MOVED |
                         PokerTableEvent.TYPE_PLAYER_AI_CHANGED);
    }

    @Override
    protected Object getDynamicTitleParam()
    {
        return title_;
    }

    @Override
    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();

        DDButton actButton_ = new GlassButton("aidoit", "Glass");
        actButton_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Phase phase = context_.getCurrentPhase();

                if (phase instanceof Bet)
                {
                    ((Bet) phase).doAI();
                }
            }
        });

        DDButton whyButton_ = new GlassButton("tellmewhy", "Glass");
        whyButton_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                context_.processPhaseNow("AdvisorInfoDialog", null);
            }
        });

        /*
        ExplicitLayout layout = new ExplicitLayout();
        base.setLayout(layout);
        */

        htmlAdvice_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        htmlAdvice_.setPreferredSize(new Dimension(100, 100));
        htmlAdvice_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        scroll_ = new DDScrollPane(htmlAdvice_, "ChatInGame", null,
                                   JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setOpaque(false);

        base.add(scroll_, BorderLayout.CENTER);

        buttons_ = new DDPanel();
        buttons_.setVisible(false);
        buttons_.setLayout(new GridLayout(1, 2, 8, 0));
        buttons_.add(whyButton_);
        buttons_.add(actButton_);
        base.add(GuiUtils.CENTER(buttons_), BorderLayout.SOUTH);

        /*
        Expression width = ContainerEF.width(base);
        Expression height = MathEF.constant(150);

        base.add(scroll_, new ExplicitConstraints(scroll_,
                        MathEF.constant(0),
                        MathEF.constant(0),
                        width,
                        height
                        ));
        layout.setPreferredLayoutSize(width, height);
        */

        return base;
    }

    @Override
    protected void updateInfo()
    {
        HoldemHand hh = game_.getCurrentTable().getHoldemHand();
        PokerPlayer pp = (hh == null) ? null : hh.getCurrentPlayer();
        Hand h = (pp == null) ? null : pp.getHand();
        PokerAI ai = (pp == null) ? null : pp.getPokerAI();

        advice_ = NOADVICE;
        title_ = NOADVICETITLE;

        if ((hh != null) && (pp != null) && (h != null) && (ai != null) &&
            !hh.isAllInShowdown() &&
            !hh.isDone() &&
            (hh.getRound() != HoldemHand.ROUND_NONE) && (hh.getRound() != HoldemHand.ROUND_SHOWDOWN) &&
            !pp.isFolded() && pp.isHumanControlled() &&
            (h.getType() == Hand.TYPE_NORMAL && !h.containsCard(Card.BLANK)) &&
            (ai instanceof V2Player))
        {
            V2Player p = (V2Player) ai;
            //if (p.isReady())
            {
                RuleEngine re = p.getRuleEngine();
                re.execute(p);
                table_ = pp.getTable();
                advice_ = re.toHTML(p.getPokerPlayer(), true, true);
                title_ = PropertyConfig.getMessage("msg.advisor.action." +
                                                   HandAction.getActionName(re.getAction().getType()));
            }
        }

        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        setTitle(getTitle());
                        htmlAdvice_.setText(advice_);
                        buttons_.setVisible(!NOADVICE.equals(advice_));
                    }
                });
    }

    @Override
    public int getPreferredBodyHeight()
    {
        return 100;
    }
}
