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
 * GuiManager.java
 *
 * Created on November 17, 2002, 3:04 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.config.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Doug Donohoe
 */
public class GuiManager implements MouseListener
{
    //static Logger logger = LogManager.getLogger(GuiManager.class);

    private static final GuiManager manager_ = new GuiManager();
    public static final String DEFAULT = "default";

    /**
     * Creates a new instance of GuiManager
     */
    private GuiManager()
    {
    }

    /**
     * init component with colors and fonts
     */
    public static void init(DDComponent dd, String sName, String sStyleName)
    {
        String sType = dd.getType();
        dd.setName(sName);

        setDefaultTooltip(dd);

        if (!(dd instanceof DDImageButton) &&
            !(dd instanceof DDScrollPane))
        {
            if (!(dd instanceof GlassButton))
            {
                dd.setBackground(StylesConfig.getColor(sStyleName + "." + sType + ".bg", dd.getBackground()));
            }

            if (!(dd instanceof PillPanel) && !(dd instanceof DDSplitPane))
            {
                dd.setForeground(StylesConfig.getColor(sStyleName + "." + sType + ".fg", dd.getForeground()));
            }
        }

        if (dd instanceof DDTextVisibleComponent && !(dd instanceof DDImageButton))
        {
            DDTextVisibleComponent text = (DDTextVisibleComponent) dd;
            text.setFont(StylesConfig.getFont(sStyleName + "." + sType, text.getFont()));
        }

        if (dd instanceof DDTextField)
        {
            DDTextField text = (DDTextField) dd;
            text.setErrorBackground(StylesConfig.getColor(sStyleName + "." + sType + ".error.bg",
                                                          text.getErrorBackground()));
        }

        if (dd instanceof DDTextArea)
        {
            DDTextArea text = (DDTextArea) dd;
            text.setErrorBackground(StylesConfig.getColor(sStyleName + "." + sType + ".error.bg",
                                                          text.getErrorBackground()));
        }

        if (dd instanceof DDNumberSpinner)
        {
            DDNumberSpinner spin = (DDNumberSpinner) dd;
            spin.setErrorBackground(StylesConfig.getColor(sStyleName + "." + sType + ".error.bg",
                                                          spin.getErrorBackground()));
        }

        setLabel(dd, sName);

        if (dd instanceof DDHasLabelComponent && !(dd instanceof DDImageButton))
        {
            DDHasLabelComponent label = (DDHasLabelComponent) dd;
            label.setText(PropertyConfig.getStringProperty(sType + "." + sName + ".label", BaseFrame.EMPTY));
        }

        if (dd instanceof DDExtendedComponent && !(dd instanceof DDImageButton))
        {
            DDExtendedComponent extended = (DDExtendedComponent) dd;
            extended.setMouseOverForeground(StylesConfig.getColor(sStyleName + "." + sType + ".mouseover.fg",
                                                                  extended.getMouseOverForeground()));
        }

        if (dd instanceof DDListComponent)
        {
            DDListComponent list = (DDListComponent) dd;
            list.setSelectionForeground(StylesConfig.getColor(sStyleName + "." + sType + ".selection.fg",
                                                              list.getSelectionForeground()));
            list.setSelectionBackground(StylesConfig.getColor(sStyleName + "." + sType + ".selection.bg",
                                                              list.getSelectionBackground()));
        }

        if (dd instanceof DDButton)
        {
            DDButton button = (DDButton) dd;
            button.setBackgroundImage(ImageConfig.getBufferedImage(sStyleName + "." + sType + ".bg", false));
        }

        if (dd instanceof GlassButton)
        {
            GlassButton glass = (GlassButton) dd;
            glass.setOverColor(StylesConfig.getColor(sStyleName + "." + sType + ".over.bg",
                                                     glass.getOverColor()));
            glass.setDownColor(StylesConfig.getColor(sStyleName + "." + sType + ".down.bg",
                                                     glass.getDownColor()));
            glass.setDisabledColor(StylesConfig.getColor(sStyleName + "." + sType + ".disable.bg",
                                                         glass.getDisabledColor()));
            glass.setFocusColor(StylesConfig.getColor(sStyleName + "." + sType + ".focus.bg",
                                                      glass.getFocusColor()));
        }

        if (dd instanceof PillColors)
        {
            PillColors pp = (PillColors) dd;
            pp.setGradientFrom(StylesConfig.getColor(sStyleName + "." + sType + ".gradfrom",
                                                     pp.getGradientFrom()));
            pp.setGradientTo(StylesConfig.getColor(sStyleName + "." + sType + ".gradto",
                                                   pp.getGradientTo()));
        }

        if (dd instanceof DDCheckBox && !(dd instanceof DDImageCheckBox))
        {
            DDCheckBox box = (DDCheckBox) dd;
            box.setCheckBoxColor(StylesConfig.getColor(sStyleName + "." + sType + ".check", box.getForeground()));
        }

        if (dd instanceof DDRadioButton)
        {
            DDRadioButton radio = (DDRadioButton) dd;
            radio.setDotColor(StylesConfig.getColor(sStyleName + "." + sType + ".dot", radio.getForeground()));
        }

        if (dd instanceof DDScrollBar)
        {
            DDScrollBar sb = (DDScrollBar) dd;
            sb.setThumbFocusColor(StylesConfig.getColor(sStyleName + "." + sType + ".focus", null));
        }

        if (dd instanceof DDSplitPane)
        {
            DDSplitPane sp = (DDSplitPane) dd;
            sp.setThumbFocusColor(StylesConfig.getColor(sStyleName + "." + sType + ".focus", null));
        }

        if (dd instanceof DDSlider)
        {
            DDSlider sl = (DDSlider) dd;
            sl.setThumbFocusColor(StylesConfig.getColor(sStyleName + "." + sType + ".thumb.focus", null));
            sl.setThumbBackgroundColor(StylesConfig.getColor(sStyleName + "." + sType + ".thumb.bg", null));
        }

        if (dd instanceof DDScrollPane)
        {
            DDScrollPane scroll = (DDScrollPane) dd;
            JViewport viewport = scroll.getViewport();
            viewport.setOpaque(true);
            viewport.setBackground(StylesConfig.getColor(sStyleName + "." + sType + ".bg", viewport.getBackground()));
        }

        if (dd instanceof DDTabbedPane)
        {
            DDTabbedPane tab = (DDTabbedPane) dd;
            Color color = StylesConfig.getColor(sStyleName + "." + sType + ".selected");
            if (color != null) tab.setSelectedTabColor(color);
        }

        if (dd instanceof DDTable)
        {
            DDTable table = (DDTable) dd;
            Color color = StylesConfig.getColor(sStyleName + "." + sType + ".grid");
            if (color != null) table.setGridColor(color);

            color = StylesConfig.getColor(sStyleName + "." + sType + ".header.fg");
            if (color != null) table.setHeaderForeground(color);

            color = StylesConfig.getColor(sStyleName + "." + sType + ".header.bg");
            if (color != null) table.setHeaderBackground(color);

            color = StylesConfig.getColor(sStyleName + "." + sType + ".select.fg");
            if (color != null) table.setSelectionForeground(color);

            color = StylesConfig.getColor(sStyleName + "." + sType + ".select.bg");
            if (color != null) table.setSelectionBackground(color);
        }

        dd.removeMouseListener(manager_); // in case of multiple calls to init, remove and re-add
        dd.addMouseListener(manager_);
    }


    /**
     * Add mouse listener (used to display help for whatever DDComponent
     * is in the parent hierarchy)
     */
    public static void addListeners(JComponent c)
    {
        c.addMouseListener(manager_);
    }

    /**
     * Reset the label using given params.  Assumes label definition has {0} ... {N} components.
     */
    public static void setLabelAsMessage(DDHasLabelComponent label, Object... params)
    {
        label.setText(PropertyConfig.getMessage(label.getType() + "." + label.getName() + ".label", params));
    }

    /**
     * set label
     */
    private static void setLabel(DDComponent dd, String sName)
    {
        String sType = dd.getType();
        if (dd instanceof DDHasLabelComponent && !(dd instanceof DDImageButton))
        {
            DDHasLabelComponent label = (DDHasLabelComponent) dd;
            label.setText(PropertyConfig.getStringProperty(sType + "." + sName + ".label", BaseFrame.EMPTY));
        }

        if (dd instanceof AbstractButton)
        {
            AbstractButton button = (AbstractButton) dd;
            String sMnemonic = PropertyConfig.getStringProperty(sType + "." + sName + ".mnemonic", null, false);
            if (sMnemonic != null && sMnemonic.length() > 0)
            {
                button.setMnemonic(sMnemonic.charAt(0));
            }
        }
    }

    /**
     * rename and update label
     */
    public static void rename(DDComponent dd, String sName)
    {
        dd.setName(sName);
        setLabel(dd, sName);
    }

    // small perf improvement for below
    private static StringBuilder sbHelpName = new StringBuilder();

    /**
     * Get default help message
     */
    public static String getDefaultHelp(DDComponent source)
    {
        String sHelp;
        sbHelpName.setLength(0);
        sbHelpName.append(source.getType());
        sbHelpName.append(".");
        sbHelpName.append(source.getName());
        sbHelpName.append(".help");
        sHelp = PropertyConfig.getStringProperty(sbHelpName.toString(),
                                                 null, false);
        return sHelp;
    }

    /**
     * set tooltip message
     */
    private static void setDefaultTooltip(DDComponent source)
    {
        String sHelp;
        sbHelpName.setLength(0);
        sbHelpName.append(source.getType());
        sbHelpName.append(".");
        sbHelpName.append(source.getName());
        sbHelpName.append(".tooltip");
        sHelp = PropertyConfig.getStringProperty(sbHelpName.toString(),
                                                 null, false);
        if (sHelp != null)
        {
            source.setToolTipText(sHelp);
        }
    }

    ////
    //// MouseListener
    ////

    /**
     * when get mouse entered, set help text
     */
    public void mouseEntered(MouseEvent e)
    {
        DDComponent source = GuiUtils.getDDComponent(e.getSource());

        if (source != null && source instanceof Component)
        {
            DDWindow window = GuiUtils.getHelpManager(((Component) source));
            if (window != null)
            {
                window.showHelp(source);
            }
        }
    }

    /**
     * Empty
     */
    public void mouseExited(MouseEvent e)
    {
    }

    /**
     * Empty
     */
    public void mouseClicked(MouseEvent e)
    {
    }

    /**
     * Empty
     */
    public void mousePressed(MouseEvent e)
    {
    }

    /**
     * Empty
     */
    public void mouseReleased(MouseEvent e)
    {
    }

}
