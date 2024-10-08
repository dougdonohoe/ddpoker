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
 * EngineBasePanel.java
 *
 * Created on November 15, 2002, 2:38 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

/**
 * @author Doug Donohoe
 */
public class EngineBasePanel extends JPanel
{
    static Logger logger = LogManager.getLogger(EngineBasePanel.class);

    JComponent bottom_ = null;
    Component center_ = null;
    Component focus_ = null;
    CenterLayout centerLayout_ = new CenterLayout();
    BorderLayout borderLayout_ = new BorderLayout();
    BaseFrame frame_;

    /**
     * Creates a new instance of EngineBasePanel
     */
    @SuppressWarnings({"ThisEscapedInObjectConstruction"})
    public EngineBasePanel(BaseFrame frame, GamePhase gamephase)
    {
        frame_ = frame;

        String sBackGroundImage = "engine.basepanel";
        if (gamephase != null)
        {
            sBackGroundImage = gamephase.getString("window-background", sBackGroundImage);
        }
        BufferedImage bi = ImageConfig.getBufferedImage(sBackGroundImage, false);

        if (bi != null)
        {
            ImageComponent ic;
            bottom_ = ic = new ImageComponent(sBackGroundImage, 1.0d);
            ic.setTile(true);
            setLayout(new BorderLayout());
            setOpaque(true);
            add(bottom_, BorderLayout.CENTER);

        }
        else
        {
            bottom_ = this;

        }

        setBackground(StylesConfig.getColor(sBackGroundImage, Color.black));
        setForeground(Color.white);

        if (TESTING(EngineConstants.TESTING_PERFORMANCE))
        {
            GuiUtils.addKeyAction(this, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "perf", new DebugPerf(), KeyEvent.VK_P, 0);
            GuiUtils.addKeyAction(this, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "gc", new DebugGC(), KeyEvent.VK_G, 0);
            GuiUtils.addKeyAction(this, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "objcount", new DebugCount(), KeyEvent.VK_O, 0);
        }
    }

    /**
     * Sets the current visible component
     */
    public void setCenterComponent(Component c, boolean bBorderLayout, Component cFocus)
    {
        if (center_ != null)
        {
            bottom_.remove(center_);
        }

        focus_ = cFocus;
        // BUG 26: leave focus null if null (prior, set to c)

        // debugging focus
//        if (focus_ != null)
//        {
//            focus_.addFocusListener(new GuiUtils.FocusDebugger(focus_.getName()));
//        }

        LayoutManager layout = bottom_.getLayout();

        if (bBorderLayout)
        {
            if (layout != borderLayout_)
            {
                bottom_.setLayout(borderLayout_);
            }
            bottom_.add(c, BorderLayout.CENTER);
        }
        else
        {
            if (layout != centerLayout_)
            {
                bottom_.setLayout(centerLayout_);
            }
            bottom_.add(c);
        }

        center_ = c;
        validate();
        repaint();

        // Upon change, change focus to this panel (old focus may have been
        // on widget in removed component)
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        requestFocus();
                    }
                }
        );
    }

    /**
     * Override request focus to give focus to the specified component
     */
    @Override
    public void requestFocus()
    {
        //logger.debug("Requesting focus for " + focus_.getClass().getName());
        // BUG 26 - don't set focus if null (fixes problem where multiple
        // calls to this in a row not ordered)
        if (focus_ != null)
        {
            focus_.requestFocus();
        }
    }

    /**
     * Return base frame this is in
     */
    public BaseFrame getBaseFrame()
    {
        return frame_;
    }

    // flag used in Gameboard to know when to repaint
    // mac grow box
    boolean bPainting_ = false;

    // growbox color
    private Color growColor_ = new Color(200, 200, 200, 125);

    // JDD 2019
    static boolean PAINT_GROW_BOX = true;

    /**
     * Override to paint bottom corner on mac for grow box
     */
    @Override
    public void paint(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;
        bPainting_ = true;
        super.paint(g);

        if (PAINT_GROW_BOX && Utils.ISMAC && frame_.isResizable()) // paint grow box
        {
            g.setColor(growColor_);
            int w = getWidth();
            int h = getHeight();
            int N = 5;
            for (int i = 0; i < 4; i++)
            {
                g.drawLine(w - N, h - 3, w - 3, h - N);
                g.drawLine(w - (N - 1), h - 3, w - 3, h - (N - 1));
                N += 4;
            }
        }
        //GuiUtils.printChildren(this, 0);
        bPainting_ = false;
    }

    /**
     * Called when 'p' pressed - toggles Perf on/off
     */
    private class DebugPerf extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            if (Perf.isStarted())
            {
                Perf.stop();
            }
            else
            {
                Perf.start();
            }
        }
    }

    /**
     * Called when 'o' pressed - display object count
     */
    private class DebugCount extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            Perf.displayCurrentCount();
        }
    }

    /**
     * Called when 'g' pressed - run GC
     */
    private class DebugGC extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            System.gc();
            logger.debug("Running GC....");
        }
    }
}
