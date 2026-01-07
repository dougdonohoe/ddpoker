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
/*
 * DDTextArea.java
 *
 * Created on November 16, 2002, 4:06 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.FocusManager;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;

/**
 * @author Doug Donohoe
 */
public class DDTextArea extends JTextArea implements DDTextVisibleComponent,
                                                     DDText,
                                                     DocumentListener, KeyListener,
                                                     FocusListener, MouseListener
{
    static Logger logger = LogManager.getLogger(DDTextArea.class);

    private Caret cNormal_;
    private Caret cNothing_ = new DoNothingCaret();
    private boolean bDisplayOnly_ = false;
    private boolean bTabChangesFocus_ = false;
    private Color bgNormal_;
    private Color bgError_ = Color.black;
    private Pattern pattern_;
    private boolean bValid_ = true;
    private JScrollPane scroll_ = null;
    public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 2, 0, 2);

    /**
     *
     */
    public DDTextArea()
    {
        this(GuiManager.DEFAULT);
    }

    public DDTextArea(String sName)
    {
        this(sName, GuiManager.DEFAULT);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDTextArea(String sName, String sStyleName)
    {
        this(sName, sStyleName, null);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDTextArea(String sName, String sStyleName, String sBevelStyle)
    {
        super();
        init(sName, sStyleName, sBevelStyle);
    }

    /**
     * init colors, borders, etc
     */
    private void init(String sName, String sStyleName, String sBevelStyle)
    {
        GuiManager.init(this, sName, sStyleName);
        bgNormal_ = getBackground();
        if (sBevelStyle == null)
        {
            setBorder(DDTextField.TEXTBORDER);
        }
        else
        {
            setBorder(DDTextField.createTextBorder(sBevelStyle));
        }
        cNormal_ = getCaret();
        addKeyListener(this);
        addMouseListener(this);
        addFocusListener(this);
        getDocument().addDocumentListener(this);

        if (Utils.ISMAC) JTextComponent.loadKeymap(getKeymap(), GuiUtils.MAC_CUT_COPY_PASTE, getActions());
    }

    /**
     * Override to fire prop change
     */
    @Override
    public void setText(String sMsg)
    {
        GuiUtils.requireSwingThread();

        super.setText(sMsg);
        firePropertyChange("value", null, null);
    }

    /**
     * Return our type
     */
    public String getType()
    {
        return "textarea";
    }

    /**
     * Set this text area as a display area that:
     * can't take focus, wraps words/lines, is not opaque,
     * can't drag and draw's with anti aliasing.
     */
    public void setDisplayOnly(boolean bDisplayOnly)
    {
        bDisplayOnly_ = bDisplayOnly;
        setWrapStyleWord(bDisplayOnly);
        setLineWrap(bDisplayOnly);
        setFocusable(!bDisplayOnly);
        setOpaque(!bDisplayOnly);
        setEditable(!bDisplayOnly);
        setDragEnabled(!bDisplayOnly);
        if (bDisplayOnly)
        {
            setCaret(cNothing_);
        }
        else
        {
            setCaret(cNormal_);
        }
    }

    public boolean isDisplayOnly()
    {
        return bDisplayOnly_;
    }

    // TODO: set caret separately with own style?
    @Override
    public void setForeground(Color c)
    {
        super.setForeground(c);
        this.setCaretColor(c);
    }

    public void setErrorBackground(Color c)
    {
        bgError_ = c;
    }

    public Color getErrorBackground()
    {
        return bgError_;
    }

    /**
     * Set whether tab changes focus
     */
    public void setTabChangesFocus(boolean b)
    {
        bTabChangesFocus_ = b;
    }

    /**
     * Get whether tab changes focus
     */
    public boolean getTabChangesFocus()
    {
        return bTabChangesFocus_;
    }

    /**
     * Set scroll pane used with this
     */
    public void setScrollPane(JScrollPane j)
    {
        scroll_ = j;
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    @Override
    public void repaint()
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, 0, 0, foo);
            foo.repaint(pRepaint.x, pRepaint.y, getWidth(), getHeight());
        }
        else
        {
            super.repaint();
        }
    }

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    @Override
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
        Object old = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    ////
    //// Key listener 
    ////
    private boolean keypressed = false;

    public void keyPressed(KeyEvent e)
    {

        if (bTabChangesFocus_ && e.getKeyCode() == KeyEvent.VK_TAB)
        {
            if (e.isShiftDown())
            {
                FocusManager.getCurrentManager().focusPreviousComponent();
            }
            else
            {
                FocusManager.getCurrentManager().focusNextComponent();
            }
            e.consume();
        }
        else
        {
            keypressed = true;
        }
    }

    /**
     * EMPTY *
     */
    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * used to fire "value" property change for
     * listeners who want keystroke by keystroke notification.
     * Also triggers default button when enter pressed
     */
    public void keyReleased(KeyEvent e)
    {
        if (!keypressed) return; // ignore spurious released events if we didn't see the press
        // this happens sometimes with enter for default button press
        keypressed = false;

        // notify of value change
        firePropertyChange("value", null, e);
    }

    /**
     * This class overrides caret so that no painting is done -- needed
     * for text areas where opaque=false and their parent has
     * a semi-transparent background.  Fixs a swing bug.
     */
    private class DoNothingCaret extends javax.swing.text.DefaultCaret
    {
        @Override
        public void paint(Graphics g)
        {
        }

        @Override
        protected synchronized void damage(Rectangle r)
        {
        }
    }

    /**
     * Set length limit on this field
     */
    public void setTextLengthLimit(int nLength)
    {
        getDocument().removeDocumentListener(this);
        setDocument(new LengthLimit(nLength));
        getDocument().addDocumentListener(this);
    }

    /*
    * Class used to Limit length
    */
    private class LengthLimit extends PlainDocument
    {

        private int limit;

        LengthLimit(int limit)
        {
            super();
            this.limit = limit;
        }

        @Override
        public void insertString(int offset, String str, AttributeSet attr)
                throws BadLocationException
        {
            if (str == null) return;

            if (bTabChangesFocus_ && str.length() == 1 && str.charAt(0) == '\t')
            {
                return;
            }

            if ((getLength() + str.length()) <= limit)
            {
                super.insertString(offset, str, attr);
            }
        }
    }

    /**
     * Set regexp to validate
     */
    public void setRegExp(String sPattern)
    {
        if (sPattern == null)
        {
            pattern_ = null;
        }
        else
        {
            pattern_ = Pattern.compile(sPattern);
        }
        regexpValidate();
    }

    /**
     * validate text
     */
    private void regexpValidate()
    {
        if (pattern_ != null)
        {
            String sNew = getText().trim();
            Matcher m = pattern_.matcher(sNew);
            Color current = getBackground();
            if (m.matches())
            {
                bValid_ = true;
                if (current != bgNormal_)
                {
                    setBackground(bgNormal_);
                    if (scroll_ != null) scroll_.setBackground(bgNormal_);
                    repaint();
                }
            }
            else
            {
                bValid_ = false;
                if (current != bgError_)
                {
                    setBackground(bgError_);
                    if (scroll_ != null) scroll_.setBackground(bgError_);
                    repaint();
                }
            }

        }
    }

    /**
     * Is this valid w.r.t. the regexp?
     */
    public boolean isValidData()
    {
        return bValid_;
    }

    ////
    //// DocumentListener methods
    ////

    /**
     * calls regexpValidate()
     */
    public void changedUpdate(DocumentEvent e)
    {
        regexpValidate();
    }

    /**
     * calls regexpValidate()
     */
    public void insertUpdate(DocumentEvent e)
    {
        regexpValidate();
    }

    /**
     * calls regexpValidate()
     */
    public void removeUpdate(DocumentEvent e)
    {
        regexpValidate();
    }

    /**
     * Invoked when a component gains the keyboard focus.
     */
    public void focusGained(FocusEvent e)
    {
        if (!bMouse_ && !isDisplayOnly() && isEnabled() && isEditable()) selectAll();
    }

    /**
     * Invoked when a component loses the keyboard focus.
     */
    public void focusLost(FocusEvent e)
    {
        // make sure value saved when focus lost
        firePropertyChange("value", null, e);
    }

    boolean bMouse_ = false;

    public void mouseClicked(MouseEvent e)
    {
        bMouse_ = false;
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        bMouse_ = true;
    }

    public void mouseReleased(MouseEvent e)
    {
        bMouse_ = false;
    }
}
