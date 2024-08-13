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
 * TerritoryPointChooser.java
 *
 * Created on October 31, 2002, 3:27 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
    
/**
 *
 * @author  Doug Donohoe
 */
public class TerritoryPointChooser extends InternalDialog implements ActionListener
{
    //static Logger logger = Logger.getLogger(TerritoryPointChooser.class);
    
    String sChoice_;
    
    /** 
     * Creates a new TerritoryPointChooser 
     */
    public TerritoryPointChooser(BaseFrame frame, String sTitle) 
    {
        super(frame, "TerritoryPointChooser", sTitle, true);
        
        setContentPane(createUI());
        validate();
        pack();
    }
     
    DDComboBox combo_;
    JButton button_ok;
    JButton button_cancel;
    
    /**
     * Create components of this chooser
     */
    private JComponent createUI()
    {
        // combobox
        combo_ = new DDComboBox("territoryPointType");
        
        JPanel combopanel = new JPanel();
        combopanel.setLayout(new BorderLayout());
        combopanel.add(combo_, BorderLayout.CENTER);

        // buttons
        JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new FlowLayout());
        
        button_ok = new JButton("OK");
        button_cancel = new JButton("Cancel");
        buttonpanel.add(button_ok);
        buttonpanel.add(button_cancel);
        button_ok.addActionListener(this);
        button_cancel.addActionListener(this);
        
        // basepanel
        JPanel basepanel = new JPanel();
        basepanel.setLayout(new BorderLayout());
        basepanel.add(combopanel, BorderLayout.CENTER);
        basepanel.add(buttonpanel, BorderLayout.SOUTH);
        
        // this frame
        combopanel.setPreferredSize(new Dimension(100,25));
        setMinimumSize(new Dimension(250, 150));
        
        return basepanel;
    }

    /**
     * set territory - means updating model so existing points
     * don't show up as a choice
     */
    public void setTerritory(Territory t)
    {
        TerritoryPoints points = t.getTerritoryPoints();

        combo_.resetValues();
        for (int i = 0; i < points.size(); i++)
        {
            combo_.removeValue(t.getTerritoryPoint(i).getType());
        }
    }
    
    /**
     * Handle button press
     */
    public void actionPerformed(ActionEvent e) 
    {
        if (e.getSource() == button_cancel)
        {
            sChoice_ = null;
            removeDialog();
        }
        else if (e.getSource() == button_ok)
        {
            sChoice_ = (String) combo_.getSelectedItem();
            removeDialog();
        }
    }
    
    /**
     * Use chooser to get a territory point modally
     */
    public static String getTerritoryPointType(TerritoryPointChooser chooser, 
                                        Territory t, int x, int y)
    {
        // figure out best location so not to cover x,y point with buffer of 10 pixels
        chooser.setNotObscurredLocation(x,y,10);
        
        // update territory chooser is using
        chooser.setTerritory(t);
        
        // set visible / modal
        chooser.showDialog(null, InternalDialog.POSITION_CENTER);//TODO - fix InternalDialog.POSITION_NOT_OBSCURRED);
        return chooser.sChoice_;
    }
}
