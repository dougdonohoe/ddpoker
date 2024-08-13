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
 * DDTextField.java
 *
 * Created on November 16, 2002, 4:06 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;

/**
 * @author Doug Donohoe
 */
public class DDTextField extends JFormattedTextField implements DDTextVisibleComponent, DDText,
                                                                KeyListener, DocumentListener,
                                                                FocusListener, MouseListener,
                                                                DDCustomHelp
{
    static Logger logger = Logger.getLogger(DDTextField.class);

    // default border used for all text fields
    public static final Border TEXTBORDER = createTextBorder(null);

    // disable bg overlay
    public static final Color DISABLE_BG = new Color(204, 204, 204, 178);

    /**
     * Create a text border with given DDBevelBorder style
     */
    public static Border createTextBorder(String sBevelStyle)
    {
        Border bb = null;
        if (sBevelStyle != null)
        {
            bb = new DDBevelBorder(sBevelStyle, BevelBorder.LOWERED);
        }
        else
        {
            bb = BorderFactory.createLoweredBevelBorder();
        }
        return BorderFactory.createCompoundBorder(
                bb,
                BorderFactory.createEmptyBorder(0, 2, 0, 2)
        );
    }

    // members
    private Caret cNormal_;
    private Caret cNothing_ = new DoNothingCaret();
    private boolean bDisplayOnly_ = false;
    private Color bgNormal_;
    private Color bgError_ = Color.black;
    private Pattern pattern_;
    private boolean bValid_ = true;
    private JButton defaultOverride_ = null;
    private boolean bOpaque_ = true;

    /**
     *
     */
    public DDTextField()
    {
        this(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }

    /**
     * new text given a name
     */
    public DDTextField(String sName)
    {
        this(sName, GuiManager.DEFAULT);
    }

    /**
     * new text with name/style
     */
    public DDTextField(String sName, String sStyleName)
    {
        this(sName, sStyleName, null);
    }

    /**
     * new text with name/style/bevel'd border style
     */
    public DDTextField(String sName, String sStyleName, String sBevelStyle)
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
        initBG();
        if (sBevelStyle == null)
        {
            setBorder(TEXTBORDER);
        }
        else
        {
            setBorder(createTextBorder(sBevelStyle));
        }
        cNormal_ = getCaret();
        setFocusLostBehavior(JFormattedTextField.PERSIST);
        addKeyListener(this);
        addFocusListener(this);
        addMouseListener(this);
        getDocument().addDocumentListener(this);
        setDisabledTextColor(GuiUtils.COLOR_DISABLED_TEXT);
        super.setOpaque(false); // w.r.t. super class, we are opaque

        if (Utils.ISMAC) JTextComponent.loadKeymap(getKeymap(), GuiUtils.MAC_CUT_COPY_PASTE, getActions());
    }

    /**
     * init bgNormal color
     */
    public void initBG()
    {
        bgNormal_ = getBackground();
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
        return "textfield";
    }

    /**
     * Set this text area as a display area that:
     * can't take focus, wraps words/lines, is not opaque,
     * can't drag and draw's with anti aliasing.
     */
    public void setDisplayOnly(boolean bDisplayOnly)
    {
        bDisplayOnly_ = bDisplayOnly;
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
     * Set button to trigger when enter pressed in field.  Overrides
     * default button
     */
    public void setDefaultOverride(JButton b)
    {
        defaultOverride_ = b;
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
     * Override to store locally since we handle opaque
     */
    @Override
    public void setOpaque(boolean b)
    {
        bOpaque_ = b;
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

        // when opaque, we paint everything except the border
        // this allows for borders with alpha to paint correctly
        // example: dd poker chat text field
        if (bOpaque_)
        {
            g.setColor(getBackground());
            g.fillRect(2, 2, getWidth() - 4, getHeight() - 4);

            if (!isEnabled())
            {
                g.setColor(DISABLE_BG);
                g.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
            }

        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    ////
    //// Key listener
    ////
    private int keypressed = 0;

    /**
     * note a key pressed
     */
    public void keyPressed(KeyEvent e)
    {
        keypressed++;
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
        // ignore spurious released events if we didn't see the press.
        // this happens sometimes with enter for default button press.
        // we use a counter since you can get press-press-release-release.
        // Don't recall issue where we get a keyReleased w/out a key-press,
        // and it might not happen anymore (JDD - 3/14/05)
        if (keypressed == 0)
        {
            return;
        }

        keypressed--;

        // notify of value change
        firePropertyChange("value", null, e);

        // if enter key, activate default button
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            JButton button = defaultOverride_;
            if (button == null)
            {
                JRootPane root = SwingUtilities.getRootPane(this);
                if (root != null)
                {
                    button = root.getDefaultButton();
                }
            }

            if (button != null && button.isEnabled())
            {
                button.doClick(120);
            }
        }
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
        setDocument(new LengthLimit(nLength));
    }

    /**
     * set document, handle listeners
     */
    @Override
    public void setDocument(Document doc)
    {
        Document current = getDocument();
        if (current != null)
        {
            current.removeDocumentListener(this);
        }
        super.setDocument(doc);
        doc.addDocumentListener(this);
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

            setValid(m.matches() && customValidate());
        }
    }

    /**
     * subclass - additional validation, for use with regexp,
     * called if regexp passes
     */
    protected boolean customValidate()
    {
        return true;
    }

    /**
     * enabled - change bg
     */
    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        setValid(isValidData());
    }

    /**
     * Set valid - for use when patterns not used
     */
    public void setValid(boolean b)
    {

        Color desired;

        if (b)
        {
            bValid_ = true;
            desired = bgNormal_;
        }
        else
        {
            bValid_ = false;
            desired = bgError_;
        }

        if (!isEnabled()) desired = bgNormal_;

        if (getBackground() != desired)
        {
            setBackground(desired);
            repaint();
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

    ////
    //// Custom help
    ////

    private String sHelp_;

    public String getHelpText()
    {
        return sHelp_;
    }

    public void setHelpText(String s)
    {
        sHelp_ = s;
    }
}
