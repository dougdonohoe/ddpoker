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
package com.donohoedigital.proto.tests;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MenuTest extends JPanel implements ActionListener, MouseListener {
   private static final long serialVersionUID = 1L;
   JPopupMenu popup = new JPopupMenu();
   JLabel messageArea = new JLabel();

   public static void main(String[] args) {
      new MenuTest();
   }

   public MenuTest() {
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      JMenuItem menuItem = new JMenuItem("kind of long popup item");
      menuItem.addActionListener(this);
      menuItem.setHorizontalAlignment(SwingConstants.CENTER);
      popup.add(menuItem);

      menuItem = new JMenuItem("very very very long popup item");
      menuItem.addActionListener(this);
      popup.add(menuItem);

      menuItem = new JMenuItem("short item");
      menuItem.addActionListener(this);
      popup.add(menuItem);

      addMouseListener(this);
      setPreferredSize(new Dimension(200, 200));
      frame.add(this,BorderLayout.CENTER);
      frame.add(messageArea,BorderLayout.SOUTH);
      frame.pack();
      frame.setLocation(200, 200);
      frame.setVisible(true);
   }

   public void actionPerformed(ActionEvent e) {
      messageArea.setText(e.getActionCommand());
   }
   public void mousePressed(MouseEvent e) {
      messageArea.setText("");
      popup.show(this,e.getX(),e.getY());
   }
   public void mouseReleased(MouseEvent e) {
      popup.setVisible(false);
   }
   public void mouseClicked(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) {}
   public void mouseExited(MouseEvent e) {}
}
