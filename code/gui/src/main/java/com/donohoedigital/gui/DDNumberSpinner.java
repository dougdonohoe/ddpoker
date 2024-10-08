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
 * DDSpinner.java
 *
 * Created on November 17, 2002, 3:47 PM
 */

package com.donohoedigital.gui;

import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDNumberSpinner extends JPanel implements ActionListener, 
                                            DDTextVisibleComponent, 
                                            PropertyChangeListener                                            
{
    static Logger logger = LogManager.getLogger(DDNumberSpinner.class);

    private int nMin_, nMax_, nStep_, nBigStep_, nValue_;
    private SpinText text_;
    private DDButton up_, upBig_;
    private DDButton down_, downBig_;
    private boolean bProtected_ = false;
    private boolean bEditable_ = true;
    private boolean bBig_;
    private DDPanel buttons_;
    private DDPanel buttonsSm_;
    private DDPanel buttonsBig_;
    private int nMaxDigits_;
    
    /** 
     * Creates a new instance of DDSpinner (value defaults to nMax)
     */
    public DDNumberSpinner(int nMin, int nMax, int nStep) {
        this(nMin, nMax, nStep, GuiManager.DEFAULT);
    }
    
    public DDNumberSpinner(int nMin, int nMax, int nStep, String sName)
    {
        this(nMin, nMax, nStep, sName, GuiManager.DEFAULT);
    }

    public DDNumberSpinner(int nMin, int nMax, int nStep, String sName, String sStyle)
    {
        super();
        setOpaque(false);
        setLayout(new BorderLayout(2,0));
        nMin_= nMin;
        nMax_ = nMax;
        nStep_ = nStep;
        nValue_ = nMin;
        nBigStep_ = nStep_ * 2;
        setMaxDigits();
        
        // text
        text_ = new SpinText();
        add(text_, BorderLayout.CENTER);
        setValue(nMax);
        
        // buttons
        buttons_ = new DDPanel();
        buttons_.setBorder(BorderFactory.createEmptyBorder(1,0,0,0));
        add(buttons_,BorderLayout.EAST);
        
        buttonsSm_ = new DDPanel();
        buttonsSm_.setLayout(new GridLayout(2,1,0,0));
        buttons_.add(buttonsSm_, BorderLayout.WEST);
        up_ = new SpinButton("up", GuiManager.DEFAULT);
        down_ = new SpinButton("down", GuiManager.DEFAULT);
        buttonsSm_.add(up_);
        buttonsSm_.add(down_);
        
        buttonsBig_ = new DDPanel();
        buttonsBig_.setLayout(new GridLayout(2,1,0,0));
        upBig_ = new SpinButton("upbig", GuiManager.DEFAULT);
        downBig_ = new SpinButton("downbig", GuiManager.DEFAULT);  
        buttonsBig_.add(upBig_);
        buttonsBig_.add(downBig_);

        // default is not editable
        setEditable(false);

        // init (after all children created)
        GuiManager.init(this, sName, sStyle); // this calls setFont(), triggering updatePreferredSizes
        text_.initBG(); // so it can get correct bg

        resetPreferredSize();
    }

    public void resetPreferredSize()
    {
        setPreferredSize(null);
        Dimension size = getPreferredSize();
        size.width += 3; // tweak
        setPreferredSize(size);
    }

    /**
     * Update preferred sizes
     */
    private void updatePreferredSizes()
    {
        Dimension size = text_.getPreferredSize();
        size.height = size.height/2 + 1;
        size.width = (int) (size.height * 1.1);
        up_.setPreferredSize(size);
        down_.setPreferredSize(size);
        
        Dimension size2 = new Dimension((int)(size.width * 1.5), size.height);
        upBig_.setPreferredSize(size2);
        downBig_.setPreferredSize(size2);
    }
        
    /** 
     * button pressed
     */
    public void actionPerformed(ActionEvent e) 
    {
        SpinButton sb = null;
        if (e.getSource() instanceof AutoTimer)
        {
            sb = ((AutoTimer) e.getSource()).spin;
        }
        else
        {
            sb = (SpinButton) e.getSource();
        }
        
        if (sb == up_)
        {
            change(nStep_);
        }
        else if (sb == upBig_)
        {
            change(nBigStep_);
        }
        else if (sb == down_)
        {
            change(-nStep_);
        }
        else if (sb == downBig_)
        {
            change(-nBigStep_);
        }
    }
    
    /**
     * change value by given amount
     */
    public void change(int nAmount)
    {
        setValue(nValue_ + nAmount);
    }
    
    /**
     * Set value
     */
    public void setValue(int nValue)
    {
        if (nValue > nMax_) nValue = nMax_;
        if (nValue < nMin_) nValue = nMin_;
        
        if (nValue == nValue_) return;
        
        setTextValue(nValue);
    }
    
    /**
     * Clear text
     */
    public void clear()
    {
        text_.setText("");
    }
    
    /**
     * Set value of text (and triggers property change,
     * which sets nValue_)
     */
    private void setTextValue(int nValue)
    {
        text_.setText(Integer.toString(nValue));
    }
    
    /** 
     * invoked when text field value changes
     */
    public void propertyChange(PropertyChangeEvent evt) 
    {
        String sText = text_.getText();
        int nNew = 0;
        if (sText != null && sText.length() > 0)
        {
            try {
                nNew = Integer.parseInt(sText);
            }
            catch (NumberFormatException nfe)
            {
                nNew = Integer.MIN_VALUE;
            }
            
        }

        // no event if no change in value
        if (nNew == nValue_) return;
        
        // check range (human entry)
        if (nNew < nMin_ || nNew > nMax_)
        {
            text_.setValid(false);
        }
        else
        {
            text_.setValid(true);
        }
        
        // all's good - new value, so fire event
        nValue_ = nNew;
        fireStateChanged();
    }
    
    /**
     * Get value
     */
    public int getValue()
    {
        return nValue_;
    }
    
    /**
     * Get min
     */
    public int getMin()
    {
        return nMin_;
    }
    
    /**
     * Set min
     */
    public void setMin(int n)
    {
        nMin_ = n;
        if (nValue_ < nMin_)
        {
            setValue(nMin_);
        }
    }
    
    /**
     * Get max
     */
    public int getMax()
    {
        return nMax_;
    }
    
    /**
     * Set max
     */
    public void setMax(int n)
    {
        nMax_ = n;
        setMaxDigits();
        if (nValue_ > nMax_)
        {
            setValue(nMax_);
        }
    }
    
    /**
     * Set length of max digits
     */
    private void setMaxDigits()
    {
        String sText = Integer.toString(nMax_);
        nMaxDigits_ = sText.length();
    }
    
    /**
     * Get step
     */
    public int getStep()
    {
        return nStep_;
    }
        
    /**
     * Set step
     */
    public void setStep(int n)
    {
        nStep_ = n;
    }
    
    /**
     * Get big step
     */
    public int getBigStep()
    {
        return nBigStep_;
    }
    
    /**
     * Set big step
     */
    public void setBigStep(int n)
    {
        nBigStep_ = n;
    }
    
    /**
     * Set to use big step
     */
    public void setUseBigStep(boolean b)
    {
        if (bBig_ == b) return;
        bBig_ = b;
        if (b)
        {
            buttons_.add(buttonsBig_, BorderLayout.CENTER);
        }
        else
        {
            buttons_.remove(buttonsBig_);
        }
        buttons_.revalidate();
        repaint();
    }
    
    /**
     * Set protected - can't edit (buttons disabled)
     */
    public void setProtected(boolean b)
    {
        if (bProtected_ == b) return;
        bProtected_ = b;
        text_.setDisplayOnly(b);
        up_.setEnabled(!b);
        down_.setEnabled(!b);
        upBig_.setEnabled(!b);
        downBig_.setEnabled(!b);
    }
    
    /**
     * Set enabled
     */
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        text_.setEnabled(b);
        up_.setEnabled(b);
        down_.setEnabled(b);
        upBig_.setEnabled(b);
        downBig_.setEnabled(b);
    }
    
    /**
     * valid?
     */
    public boolean isValidData()
    {
        if (!text_.isValidData()) return false;
        if (nValue_ < nMin_) return false;
        if (nValue_ > nMax_) return false;
        return true;
    }

    /**
     * Edits allowed?
     */
    public boolean isEditable()
    {
        return bEditable_;
    }
    
    /**
     * Set editable
     */
    public void setEditable(boolean b)
    {
        if (bEditable_ == b) return;
        bEditable_ = b;
        text_.setDisplayOnly(!b);
        text_.setOpaque(true);
    }
    
    /**
     * Get text field
     */
    public SpinText getTextField()
    {
        return text_;
    }

    /**
     * Set focus to text
     */
    public void requestFocus()
    {
        text_.requestFocus();
        text_.setCaretPosition(text_.getText().length());
    }

    /**
     * whether we have focus
     */
    public boolean hasFocus()
    {
        return text_.hasFocus();
    }

    /**
     * Also a spinner
     */
    public String getType() 
    {
        return "spinner";
    }

    /**
     * Set font for this and all children
     */
    public void setFont(Font f)
    {
        if (text_ != null)
        {
            text_.setFont(f);
            Font fSmall = f.deriveFont(f.getSize() * .5f);
            upBig_.setFont(fSmall);
            up_.setFont(fSmall);
            down_.setFont(fSmall);
            downBig_.setFont(fSmall);
            updatePreferredSizes();
        }
        super.setFont(f);
    }

    /**
     * Set background for this and all children
     */
    public void setBackground(Color c)
    {
        if (text_ != null)
        {
            text_.setBackground(c);
            up_.setBackground(c);
            upBig_.setBackground(c);
            down_.setBackground(c);
            downBig_.setBackground(c);
        }
        super.setBackground(c);
    }

    /**
     * Set foreground for this and all children
     */
    public void setForeground(Color c)
    {
        if (text_ != null)
        {
            text_.setForeground(c);
            up_.setForeground(c);
            upBig_.setForeground(c);
            down_.setForeground(c);
            downBig_.setForeground(c);
        }
        super.setForeground(c);
    }
    
    /**
     * Error bg - get rom text
     */
    public void setErrorBackground(Color c)
    {
        text_.setErrorBackground(c);
    }
    
    /**
     * error bg - pass on to text
     */
    public Color getErrorBackground()
    {
        return text_.getErrorBackground();
    }
    
    /**
     * Add mouse listener for this and all children
     */
    public void addMouseListener(MouseListener listener)
    {
        if (listener instanceof GuiManager)
        {
            GuiUtils.addMouseListenerChildren(this, listener);
        }
        super.addMouseListener(listener);
    }  
    
    /**
     * Remove mouse listener for this and all children
     */
    public void removeMouseListener(MouseListener listener)
    {
        if (listener instanceof GuiManager)
        {
            GuiUtils.removeMouseListenerChildren(this, listener);
        }
        super.removeMouseListener(listener);
    }    
    
    /**
     * Spin text class
     */
    public class SpinText extends DDTextField implements MouseWheelListener
    {
        public SpinText()
        {
            super(GuiManager.DEFAULT, GuiManager.DEFAULT);
            setDocument(new NumberOnly());
            setHorizontalAlignment(JTextField.RIGHT);
            addPropertyChangeListener("value", DDNumberSpinner.this);
            addMouseWheelListener(this);
            addArrowActions(this);
        }

        /**
         * Override to ignore non-digit replacement
         */
        public void replaceSelection(String str) 
        {
            //limit replacement to digits only
            char c;
            for (int i = 0; i < str.length(); i++)
            {
                c = str.charAt(i);
                if (c < '0' || c > '9') return;
            }
            super.replaceSelection(str);
        }
        
        /** 
         * Mouse wheel - change spinner
         */
        public void mouseWheelMoved(MouseWheelEvent e) 
        {
            if (!isEnabled()) return;

            int nRotate = e.getWheelRotation();        
            int nValue = (e.isControlDown()) ? getBigStep() : getStep();
            if (nRotate > 0) nValue *= -1;

            change(nValue);

            e.consume();
        }
    }
        
    /*
     * Class used to limit numeric spinner models to numbers
     */
    private class NumberOnly extends PlainDocument 
    {    
        public NumberOnly()
        {
            super();
        }

        public void insertString(int offset, String  str, AttributeSet attr)
                throws BadLocationException 
        {
            if (str == null) return;

            // limit to 9 digits (99,999,999)
            if ((getLength() + str.length()) > nMaxDigits_) {
                return;
            }

            // limit to digits
            char c;
            for (int i = 0; i < str.length(); i++)
            {
                c = str.charAt(i);
                if (c < '0' || c > '9') return;
            }

            super.insertString(offset, str, attr);
        }
    }

    /**
     * Add arrow keyboard actions that control this spinner to the given component
     */
    public void addArrowActions(JComponent to)
    {
        GuiUtils.addKeyAction(to, JComponent.WHEN_FOCUSED,
                     "scrollup", new ScrollUp(this), KeyEvent.VK_UP,  0);
        GuiUtils.addKeyAction(to, JComponent.WHEN_FOCUSED,
                     "scrolldown", new ScrollDown(this), KeyEvent.VK_DOWN,  0);

        // crtl-arrow keys move all the way
        GuiUtils.addKeyAction(to, JComponent.WHEN_FOCUSED,
                     "scrollupmax", new ScrollUpMax(this), KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK);
        GuiUtils.addKeyAction(to, JComponent.WHEN_FOCUSED,
                     "scrolldownmax", new ScrollDownMax(this), KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK);
    }

    /**
     * up action
     */
    private static class ScrollUp extends AbstractAction
    {
        private DDNumberSpinner spin;
        public ScrollUp(DDNumberSpinner spin)
        {
            this.spin = spin;
        }
        public void actionPerformed(ActionEvent e)
        {
            if (!spin.isEnabled() || !spin.isVisible()) return;
            spin.change(spin.nStep_);
        }
    }

     /**
     * down action
     */
    private static class ScrollDown extends AbstractAction
    {
        private DDNumberSpinner spin;
        public ScrollDown(DDNumberSpinner spin)
        {
            this.spin = spin;
        }
        public void actionPerformed(ActionEvent e)
        {
            if (!spin.isEnabled() || !spin.isVisible()) return;
            spin.change(-spin.nStep_);
        }
    }

     /**
     * up max action
     */
    private static class ScrollUpMax extends AbstractAction
    {
        private DDNumberSpinner spin;
        public ScrollUpMax(DDNumberSpinner spin)
        {
            this.spin = spin;
        }
        public void actionPerformed(ActionEvent e)
        {
            if (!spin.isEnabled() || !spin.isVisible()) return;
            spin.change(spin.nBigStep_);
        }
    }

    /**
     * down max action
     */
    private static class ScrollDownMax extends AbstractAction
    {
        private DDNumberSpinner spin;
        public ScrollDownMax(DDNumberSpinner spin)
        {
            this.spin = spin;
        }
        public void actionPerformed(ActionEvent e)
        {
            if (!spin.isEnabled() || !spin.isVisible()) return;
            spin.change(-spin.nBigStep_);
        }
    }
    
    /**
     * Spin button class
     */
    private class SpinButton extends DDButton implements MouseListener
    {
        AutoTimer autoRepeatTimer;
        SpinButton(String sName, String sStyle)
        {
            super(sName, sStyle);
            setFocusPainted(false);
            setFocusable(false);
            setBorderGap(0, 0, 0, 0);
            addActionListener(DDNumberSpinner.this);
            addMouseListener(this);
            addMouseWheelListener(text_); // BUG 359
            autoRepeatTimer = new AutoTimer(this, 2, DDNumberSpinner.this);
	        autoRepeatTimer.setInitialDelay(250);
            setAlwaysAntiAlias(true);
        }
        
        public void mousePressed(MouseEvent e) 
        {
            if (SwingUtilities.isLeftMouseButton(e) && e.getComponent().isEnabled()) 
            {
                autoRepeatTimer.start();
            }
        }

        public void mouseReleased(MouseEvent e) {
            autoRepeatTimer.stop();	    
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    /**
     * auto timer to handle held-down button
     */
    private static class AutoTimer extends javax.swing.Timer
    {
        public SpinButton spin;
        
        public AutoTimer(SpinButton button, int n, ActionListener listener)
        {
            super(n, listener);
            spin = button;
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
