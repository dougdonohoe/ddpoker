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
package com.donohoedigital.gui;

import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 19, 2005
 * Time: 10:25:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class ColorChooserPanel extends DDPanel
{
    static Logger logger = Logger.getLogger(ColorChooserPanel.class);

    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    private static final int ALPHA = 3;

    private String STYLE;
    private DDLabel sample_;
    private Color color_;
    ColorPanel red_, green_, blue_, alpha_;

    public ColorChooserPanel(String sStyle, Color cInitial, String sTile)
    {
        super(GuiManager.DEFAULT, sStyle);
        STYLE = sStyle;
        color_ = cInitial;

        createUI(sTile);
    }

    public Color getColor()
    {
        return color_;
    }

    private boolean bSetting_ = false;
    public void setColor(Color c)
    {
        color_ = c;

        bSetting_ = true;
        red_.setColor(c);
        green_.setColor(c);
        blue_.setColor(c);
        alpha_.setColor(c);
        bSetting_ = false;

        changeColor();
    }


    /**
     * Change sample color, fire state changed
     */
    private void changeColor()
    {
        sample_.setBackground(color_);
        sample_.repaint();

        // notify listeners
        fireStateChanged();
    }

    /**
     * create ui
     */
    private void createUI(String sTile)
    {
        setBorderLayoutGap(0, 10);

        // sample color
        DDPanel sample = new DDPanel();
        JComponent parent = sample;
        sample.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,5,5,5),
                new DDBevelBorder(STYLE, DDBevelBorder.LOWERED)));

        if (sTile != null)
        {
            ImageComponent tile = new ImageComponent(sTile, 1.0);
            tile.setLayout(new BorderLayout());
            tile.setTile(true);
            tile.setPreferredSize(new Dimension(25,25));
            sample.add(tile, BorderLayout.CENTER);
            parent = tile;
        }

        sample_ = new DDLabel();
        sample_.setOpaque(true);
        parent.add(sample_, BorderLayout.CENTER);
        add(sample, BorderLayout.EAST);
        sample_.setPreferredWidth(25);

        // controls
        DDPanel controls = new DDPanel();
        controls.setLayout(new GridLayout(0,1,0,-7));

        controls.add(red_ = new ColorPanel(RED));
        controls.add(green_ = new ColorPanel(GREEN));
        controls.add(blue_ = new ColorPanel(BLUE));
        controls.add(alpha_ = new ColorPanel(ALPHA));

        add(controls,BorderLayout.CENTER);

    }

    /**
     * is valid data?
     */
    public boolean isValidData()
    {
        return red_.isValidData() &&
               green_.isValidData() &&
               blue_.isValidData() &&
               alpha_.isValidData();
    }

    /**
     * update color based on slider/spinner change
     */
    private void updateColor(int nType, int nValue)
    {
        if (bSetting_) return;

        switch(nType)
        {
            case RED:
                color_ = new Color(nValue, color_.getGreen(), color_.getBlue(), color_.getAlpha());
                break;

            case GREEN:
                color_ = new Color(color_.getRed(), nValue, color_.getBlue(), color_.getAlpha());
                break;

            case BLUE:
                color_ = new Color(color_.getRed(), color_.getGreen(), nValue, color_.getAlpha());
                break;

            case ALPHA:
                color_ = new Color(color_.getRed(), color_.getGreen(), color_.getBlue(), nValue);
                break;
        }

        changeColor();

    }

    /**
     * color panel
     */
    private class ColorPanel extends DDPanel implements ChangeListener
    {
        DDSlider slider;
        DDNumberSpinner spinner;
        int nType;
        String sName;

        public ColorPanel(int type)
        {
            nType = type;
            switch(nType)
            {
                case RED:
                    sName = "red";
                    break;

                case GREEN:
                    sName ="green";
                    break;

                case BLUE:
                    sName = "blue";
                    break;

                case ALPHA:
                    sName = "alpha";
                    break;
            }

            // flow layout
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

            // label
            DDLabel label = new DDLabel(sName, STYLE);
            label.setPreferredWidth(45);
            add(label);

            // slider
            slider = new DDSlider(GuiManager.DEFAULT, STYLE);
            slider.setPreferredSize(new Dimension(100, 25));
            slider.setMinimum(0);
            slider.setMaximum(255);
            slider.addChangeListener(this);
            slider.setSnapToTicks(true);
            add(slider);

            // spinner
            spinner = new DDNumberSpinner(0, 255, 1, GuiManager.DEFAULT, STYLE);
            Dimension size = spinner.getPreferredSize();
            size.height -= 5;
            spinner.setPreferredSize(size);
            spinner.setEditable(true);
            spinner.addChangeListener(this);
            add(spinner);

            // set values
            setColor(color_);
        }

        private boolean bUpdating = false;
        public void stateChanged(ChangeEvent e)
        {
            if (bUpdating) return;
            bUpdating = true;
            if (e.getSource() == slider)
            {
                spinner.setValue(slider.getValue());
            }
            else
            {
                slider.setValue(spinner.getValue());
            }
            bUpdating = false;

            updateColor(nType, slider.getValue());
        }

        public void setColor(Color c)
        {
            int value = 0;
            switch(nType)
            {
                case RED:
                    value = c.getRed();
                    break;

                case GREEN:
                    value = c.getGreen();
                    break;

                case BLUE:
                    value = c.getBlue();
                    break;

                case ALPHA:
                    value = c.getAlpha();
                    break;
            }

            spinner.setValue(value);
            slider.setValue(value);
        }

        public boolean isValidData()
        {
            return spinner.isValidData();
        }
    }

   private transient ChangeEvent changeEvent;

   /**
     * Adds a listener to the list that is notified each time a change
     * to the model occurs.  The source of <code>ChangeEvents</code>
     * delivered to <code>ChangeListeners</code> will be this
     * <code>DDNumberSpinner</code>.
     *
     * @param listener the <code>ChangeListener</code> to add
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }


    /**
     * Removes a <code>ChangeListener</code> from this spinner.
     *
     * @param listener the <code>ChangeListener</code> to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }


    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this DDNumberSpinner with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])listenerList.getListeners(
                ChangeListener.class);
    }


    /**
     * Sends a <code>ChangeEvent</code>, whose source is this
     * <code>DDNumberSpinner</code>, to each <code>ChangeListener</code>.
     * When a <code>ChangeListener</code> has been added
     * to the spinner, this method method is called each time
     * a <code>ChangeEvent</code> is received from the model.
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }

}
