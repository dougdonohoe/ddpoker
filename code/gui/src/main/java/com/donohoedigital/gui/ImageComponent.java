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
 * ImageComponent.java
 *
 * Created on October 31, 2002, 7:51 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
public class ImageComponent extends JComponent implements Icon
{

    static Logger logger = Logger.getLogger(ImageComponent.class);

    protected String sName_;
    protected BufferedImage bimage_;
    protected boolean bComposite_ = false;
    protected BufferedImage composites_[];
    protected int compositeXs_[];
    protected int compositeYs_[];
    protected int nCompositeHeight_, nCompositeWidth_;
    protected Image grayimage_;
    protected Image hiliteimage_;
    protected boolean bHighlighted_ = false;
    protected double dScaleFactor_ = 1.0;

    private Dimension dInitialSize_;
    private boolean scaleToFit_ = true;
    private boolean bTile_ = false;
    private Rectangle imagebounds_;
    private boolean bHidden_ = false;
    private CustomImage custom_;
    private boolean bUseCustom_ = false;
    private ImageComponent parentTile_ = null;
    private ComponentAdapter parentAdapter_ = null;
    private boolean bBuffer_ = false;
    private boolean bRefreshBuffer_ = false;
    private BufferedImage buffer_ = null;
    private boolean bCenter_ = true;
    static int nColor_ = -1;

    /**
     * For subclass
     */
    protected ImageComponent()
    {
    }

    /**
     * New ImageComponent from image name (lookup in ImageDef)
     * and given scale
     */
    public ImageComponent(String sName, double dScaleFactor)
    {
        sName_ = sName;
        dScaleFactor_ = dScaleFactor;
        ImageDef def = ImageConfig.getImageDef(sName);
        if (def.isComposite())
        {
            bComposite_ = true;
            ImageDef composite;
            String[] names = def.getComponents();
            int nNum = names.length;
            composites_ = new BufferedImage[nNum];
            compositeXs_ = new int[nNum];
            compositeYs_ = new int[nNum];
            for (int i = 0; i < nNum; i++)
            {
                composite = ImageConfig.getImageDef(names[i]);
                if (composite == null)
                {
                    throw new ApplicationError(ErrorCodes.ERROR_FILE_NOT_FOUND, "Composite component not found: " + names[i], (String) null, null);
                }
                composites_[i] = composite.getBufferedImage();
                compositeXs_[i] = composite.getX();
                compositeYs_[i] = composite.getY();
            }
            nCompositeWidth_ = def.getX();
            nCompositeHeight_ = def.getY();
        }
        else
        {
            bimage_ = ImageConfig.getBufferedImage(sName);
        }
        init();
    }

    /**
     * New ImageComponent from buffered image
     */
    public ImageComponent(BufferedImage image, double dScaleFactor)
    {
        sName_ = "<none>";
        dScaleFactor_ = dScaleFactor;
        bimage_ = image;
        init();
    }

    /**
     * Create new ImageComponent from an existing one
     */
    public ImageComponent(ImageComponent copy)
    {
        sName_ = copy.sName_;
        dScaleFactor_ = copy.dScaleFactor_;
        bimage_ = copy.bimage_;
        init();
    }

    /**
     * Change image used in this imagecomponent
     */
    public void changeName(String sName)
    {
        bimage_ = ImageConfig.getBufferedImage(sName);
        dInitialSize_ = new Dimension((int) (getImageWidth() * dScaleFactor_), (int) (getImageHeight() * dScaleFactor_));
        setPreferredSize(dInitialSize_);
        imagebounds_ = new Rectangle(0, 0, getImageWidth(), getImageHeight());
    }

    /**
     * if the image has transparent bits, it may be drawn over a
     * parent that is tiled.  That image is specified here, and
     * must be used with setBuffer(true), as the parent's image
     * isn't painted in non-bufferred mode.  See PokerGameboard
     * for an example.
     */
    public void setParentTile(ImageComponent ic)
    {
        // cleanup if reseting tile
        if (parentTile_ != null)
        {
            parentTile_.removeComponentListener(parentAdapter_);
        }
        parentTile_ = ic;

        // need to reset buffer if parent resizes
        parentAdapter_ = new ComponentAdapter()
        {
            public void componentResized(ComponentEvent event)
            {
                bRefreshBuffer_ = true;
            }
        };
        parentTile_.addComponentListener(parentAdapter_);
    }

    /**
     * get parent which is used to tile transparent parts of image
     */
    public ImageComponent getParentTile()
    {
        return parentTile_;
    }

    /**
     * set to use custom image
     */
    public void setUseCustom(boolean b)
    {
        bUseCustom_ = b;
    }

    /**
     * is custom used?
     */
    public boolean isUseCustom()
    {
        return bUseCustom_;
    }

    /**
     * Custom image drawer
     */
    public void setCustomImage(CustomImage custom)
    {
        custom_ = custom;
    }

    /**
     * Get custom image
     */
    public CustomImage getCustomImage()
    {
        return custom_;
    }

    /**
     * set whether to buffer the image.  if the
     * image is large, painted often and scaled,
     * it can be a big performance improvment to
     * buffer the scaled image.
     */
    public void setBuffer(boolean b)
    {
        bBuffer_ = b;
    }

    /**
     * is bufferring?
     */
    public boolean isBuffer()
    {
        return bBuffer_;
    }

    /**
     * Set whether to refresh buffer on next repaint
     */
    public void setRefreshBuffer(boolean b)
    {
        bRefreshBuffer_ = b;
    }

    /**
     * Set whether image is centered (if not scaled).  Default is true
     */
    public void setCentered(boolean b)
    {
        bCenter_ = b;
    }

    /**
     * Is centered?
     */
    public boolean isCentered()
    {
        return bCenter_;
    }

    /**
     * Set size, image bounds
     */
    protected void init()
    {
        //logger.debug("New image: " + sName_);
        dInitialSize_ = new Dimension((int) (getImageWidth() * dScaleFactor_), (int) (getImageHeight() * dScaleFactor_));

        setPreferredSize(dInitialSize_);
        setSize(dInitialSize_);

        imagebounds_ = new Rectangle(0, 0, getImageWidth(), getImageHeight());
    }

    /**
     * Get width of original image
     */
    public int getImageWidth()
    {
        if (bComposite_) return nCompositeWidth_;
        return bimage_.getWidth();
    }

    /**
     * Get height of original image
     */
    public int getImageHeight()
    {
        if (bComposite_) return nCompositeHeight_;
        return bimage_.getHeight();
    }

    /**
     * call static version of this method, bassing our BufferedImage
     */
    public boolean isNonTransparent(int imagex, int imagey)
    {
        if (bComposite_)
            throw new UnsupportedOperationException("isNotTransparent() not supported with composite images");

        return isNonTransparent(bimage_, imagex, imagey);
    }

    /**
     * Return true if given x,y in image is non-transparent,
     * meaning there is some color, even if not completely opaque
     */
    public static boolean isNonTransparent(BufferedImage bimage, int imagex, int imagey)
    {
        // BUG 237 - don't check if we would get a out of bounds exception
        if (imagex < 0 || imagex >= bimage.getWidth() ||
            imagey < 0 || imagey >= bimage.getHeight()) return false;

        int nColor;
        int alpha = 0;
        try
        {
            nColor = bimage.getRGB(imagex, imagey);
            alpha = (nColor >> 24) & 0xff;
        }
        // catch this just in case
        catch (ArrayIndexOutOfBoundsException out)
        {
            logger.warn("Caught the out-of-bounds exception in findPieceInTerritory: " +
                        " imagex=" + imagex + " imagey=" + imagey +
                        " imagewidth=" + bimage.getWidth() + " imageheight=" + bimage.getHeight());
            alpha = 0;
        }

        return alpha > 0;
    }

    /**
     * Set hidden
     */
    public void setHidden(boolean b)
    {
        bHidden_ = b;
    }

    /**
     * is hidden?
     */
    public boolean isHidden()
    {
        return bHidden_;
    }

    /**
     * Reset scale
     */
    public void setScale(double dScaleFactor)
    {
        dScaleFactor_ = dScaleFactor;
        init();
    }

    /**
     * Get scale
     */
    public double getScale()
    {
        return dScaleFactor_;
    }

    /**
     * Get image used by this
     */
    public BufferedImage getImage()
    {
        if (bComposite_) throw new UnsupportedOperationException("getImage() not supported with composite images");

        return bimage_;
    }

    /**
     * Set whether image is scaled to fit size of this component.  Default is true
     */
    public void setScaleToFit(boolean b)
    {
        scaleToFit_ = b;
    }

    /**
     * Get whether image is scaled to fit size of this component
     */
    public boolean isScaleToFit()
    {
        return scaleToFit_;
    }

    /**
     * Set whether the image should be tiled if display area
     * is larger than image (this method sets setScaleToFit to !bTile)
     */
    public void setTile(boolean bTile)
    {
        setScaleToFit(!bTile);
        bTile_ = bTile;
    }

    /**
     * Get whether tiling
     */
    public boolean getTile()
    {
        return bTile_;
    }

    /**
     * Set the bounds of the image we should use (in the coordinate
     * space of the original image).  Used to clip areas of image.
     * Only used if isScaleToFit() is true.
     */
    protected void setImageBounds(Rectangle bounds)
    {
        imagebounds_ = bounds;
    }

    /**
     * Get bounds used to draw this image
     */
    public Rectangle getImageBounds()
    {
        return imagebounds_;
    }

    // use for performance
    // BUG 133 - static okay
    private static Rectangle bounds_ = new Rectangle();

    /**
     * Paint this component
     */
    protected void paintComponent(Graphics g1)
    {
        if (bHidden_) return;

        Graphics2D g = (Graphics2D) g1;
        g.getClipBounds(bounds_);
        Image drawThis = whatToDraw();

        // draw image scaled to fit in current size, adjusted for image bounds changes
        if (scaleToFit_)
        {
            if (custom_ != null && bUseCustom_)
            {
                custom_.paintCustom(g, 0, 0, getWidth(), getHeight());
            }
            else
            {
                //logger.debug("IMAGECOMPONENT " + sName_ + " size: " + getWidth() + "," + getHeight()
                //             + " - bounds: " + bounds_ + " imagebounds: " + imagebounds_);
                if (bBuffer_)
                {
                    //logger.debug("IMAGECOMPONENT " + sName_ + " size: " + getWidth() + "x" + getHeight()
                    //             + (buffer_ == null ? " [null buffer" : " [buffer: " + buffer_.getWidth() + "x" + buffer_.getHeight()) +
                    //             "] refresh: " + bRefreshBuffer_);
                    boolean bSizeChanged = buffer_ == null || (buffer_.getWidth() != getWidth() || buffer_.getHeight() != getHeight());
                    if (bSizeChanged || bRefreshBuffer_)
                    {
                        bRefreshBuffer_ = false;
                        if (bSizeChanged)
                        {
                            buffer_ = null; // null out in case GC needed when creating new buffer
                            //LoggingConfig.debugPrintMemory("Before new buffer");
                            int estimate = getWidth() * getHeight() * 32 / 8;
                            long free = Runtime.getRuntime().freeMemory();
                            if (free < estimate)
                            {
                                logger.warn(PropertyConfig.getMessage("msg.needmem.debug", new Integer(estimate), new Long(free)));
                            }
                            buffer_ = ImageDef.createBufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                            //LoggingConfig.debugPrintMemory("After new buffer");
                        }
                        Graphics2D gBuffer = (Graphics2D) buffer_.getGraphics();

                        // if a parentTile is specified, paint that first
                        paintParentTile(gBuffer);

                        // draw the image
                        renderImage(gBuffer, drawThis);
                    }
                    g.drawImage(buffer_, 0, 0, this);
                }
                else
                {
                    renderImage(g, drawThis);
                }
            }

        }
        // draw image in size specified at creation, or tiled.
        // note: this does not use imagebounds (as of now)
        else
        {
            // if not tiling, just draw image
            if (!bTile_)
            {
                int y = 0;
                int x = 0;
                if (bCenter_ && getHeight() > dInitialSize_.height)
                {
                    y = (getHeight() - dInitialSize_.height) / 2;
                }
                if (bCenter_ && getWidth() > dInitialSize_.width)
                {
                    x = (getWidth() - dInitialSize_.width) / 2;
                }

                // note: this scales image to initial width/height if necessary
                g.drawImage(drawThis, x, y, dInitialSize_.width, dInitialSize_.height, this);
            }
            // else tiling, repeat image until display area filled
            else
            {
                tile(g, drawThis, bounds_);
            }
        }
    }

    /**
     * render image
     */
    protected void renderImage(Graphics2D g, Image drawThis)
    {
        if (!bComposite_)
        {
            g.drawImage(drawThis, 0, 0, getWidth(), getHeight(),
                        imagebounds_.x, imagebounds_.y,
                        (imagebounds_.x + imagebounds_.width), (imagebounds_.y + imagebounds_.height),
                        this);
        }
        else
        {
            renderComposite(g);
        }
    }

    /**
     * render composite
     */
    private void renderComposite(Graphics2D g)
    {
        BufferedImage img;
        double origWidth = getImageWidth();
        double origHeight = getImageHeight();
        double x, y, w, h;
        double drawWidth = getWidth();
        double drawHeight = getHeight();
        double newX, newY, newW, newH;
        //logger.debug("Orig size: " + origWidth +"x"+origHeight + " drawing to " + drawWidth+"x"+drawHeight);
        for (int i = 0; i < composites_.length; i++)
        {
            img = composites_[i];
            x = compositeXs_[i];
            y = compositeYs_[i];
            w = img.getWidth();
            h = img.getHeight();

            newX = x * drawWidth / origWidth;
            newY = y * drawHeight / origHeight;
            newW = w * drawWidth / origWidth;
            newH = h * drawHeight / origHeight;

            //logger.debug(i + " pos: " + x+","+y+" size: " + w+"x"+h+"  drawat: "+
            //                            newX+","+newY+" size: " + newW+"x"+newH);
            g.drawImage(img, (int) Math.round(newX), (int) Math.round(newY),
                        (int) Math.ceil(newW), (int) Math.ceil(newH), this);
        }
    }

    /**
     * paints parent's tile pattern into this image
     */
    protected void paintParentTile(Graphics2D g)
    {
        if (parentTile_ == null) return;

        Point pRepaint = SwingUtilities.convertPoint(this, 0, 0, parentTile_);
        Rectangle bounds = new Rectangle(0, 0,
                                         getWidth() + pRepaint.x,
                                         getHeight() + pRepaint.y);
        g.translate(-pRepaint.x, -pRepaint.y);
        parentTile_.tile(g, parentTile_.whatToDraw(), bounds);
        g.translate(pRepaint.x, pRepaint.y);
    }

    /**
     * Tile logic
     */
    private void tile(Graphics2D g, Image drawThis, Rectangle bounds)
    {
        // draw only what is needed by clipbounds (this saves drawing
        // if repainting near top left of image area
        int width = bounds.width + bounds.x;
        int height = bounds.height + bounds.y;
        int xStart = bounds.x - (bounds.x % dInitialSize_.width);
        int yStart = bounds.y - (bounds.y % dInitialSize_.height);

        //logger.debug("Retiling " + bounds_+" starting: " + xStart+","+yStart);
        int x, y;
        int drawwidth = 0;
        int drawheight = 0;
        int widthdrawn = 0;
        int heightdrawn = yStart;

        // draw height
        y = yStart;
        while (heightdrawn < height)
        {
            drawheight = dInitialSize_.height;
            if (drawheight > (height - heightdrawn))
            {
                drawheight = height - heightdrawn;
            }
            widthdrawn = xStart;
            x = xStart;
            // draw width
            while (widthdrawn < width)
            {

                drawwidth = dInitialSize_.width;
                if (drawwidth > (width - widthdrawn))
                {
                    drawwidth = width - widthdrawn;
                }

//                logger.debug("drawing at " + x + "," + y + " " + drawwidth + "x" + drawheight + " " +
//                             getDebugColorName());

                g.drawImage(drawThis, x, y, this);
                // JDD - okay to just draw image without defining draw height
                // x+drawwidth, y+drawheight,
                // 0, 0, drawwidth, drawheight, this);

//                Color c = getDebugColor();
//                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 75);
//                g.setColor(c);
//                g.fillRect(x,y,x+drawwidth,y+drawheight);

                widthdrawn += drawwidth;
                x += drawwidth;
            }
            heightdrawn += drawheight;
            y += drawheight;
        }
    }

    /**
     * Draw this image at the given coordinates, scaled to the width/height.
     * Used to draw the component when it is not part of a hierarchy
     */
    public void drawImageAt(Graphics2D g, int x, int y, int width, int height)
    {
        if (bHidden_) return;

        Image drawThis = whatToDraw();
        //logger.debug("drawing image at " + x+","+y+" " + width + "x"+height);
        drawImageAt(g, drawThis, x, y, width, height);
    }

    /**
     * Draw given image at given location
     */
    private void drawImageAt(Graphics2D g, Image image, int x, int y, int width, int height)
    {
        g.drawImage(image, x, y, (x + width), (y + height),
                    imagebounds_.x, imagebounds_.y,
                    (imagebounds_.x + imagebounds_.width), (imagebounds_.y + imagebounds_.height),
                    this);
    }

    /**
     * Return image to draw based on enabled/highlight settings
     */
    private Image whatToDraw()
    {
        // if grayed out, return that as 1st choice
        // otherwise, return selectedimage if selected
        // if not selected, return regular image
        return (!isEnabled()) ? getDisabledImage() :
               (bHighlighted_) ? hiliteimage_ : bimage_;
    }

    /**
     * Set highlighted.  If setting to true, the highlighted image is created
     * on demand (if not previously created)
     */
    public void setHighlighted(boolean b)
    {
        if (bComposite_)
            throw new UnsupportedOperationException("setHighlighted() not supported with composite images");

        bHighlighted_ = b;
        if (b)
        {
            if (hiliteimage_ == null && bimage_ != null)
            {
                hiliteimage_ = createHighlightImage(bimage_);
            }
        }
    }

    /**
     * Is this image selected?
     */
    public boolean isHighlighted()
    {
        return bHighlighted_;
    }

    /**
     * Create new disabled image (not cached)
     */
    private Image getDisabledImage()
    {
        if (bComposite_)
            throw new UnsupportedOperationException("getDisabledImage() not supported with composite images");

        if (grayimage_ == null && bimage_ != null)
        {
            grayimage_ = GrayFilter.createDisabledImage(bimage_);
        }
        return grayimage_;
    }

    /**
     * Get image for given name/scale.  Images are cached, so this
     * should only be used for items that are reused frequently.
     */
    public static ImageComponent getImage(String sName, double dScale)
    {
        String sKey = sName + "." + dScale;

        ImageComponent ic;
        ic = (ImageComponent) images_.get(sKey);
        if (ic == null)
        {
            ic = new ImageComponent(sName, dScale);
            images_.put(sKey, ic);
        }
        return ic;
    }

    private static HashMap images_ = new HashMap();

    ////
    //// image filter for highlight
    ////

    /**
     * Create image used to highlight this one
     */
    private Image createHighlightImage(Image i)
    {
        CyanFilter filter = new CyanFilter();
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);
        return grayImage;
    }

    public static Color getDebugColor()
    {
        switch (nColor_)
        {
            case 0:
                return Color.red;
            case 1:
                return Color.CYAN;
            case 2:
                return Color.blue;
            case 3:
                return Color.magenta;
            case 4:
                return Color.yellow;
            case 5:
                return Color.green;
        }
        return Color.black;
    }

    public static String getDebugColorName()
    {
        nColor_++;
        if (nColor_ == 6) nColor_ = 0;
        switch (nColor_)
        {
            case 0:
                return "red";
            case 1:
                return "cyan";
            case 2:
                return "blue";
            case 3:
                return "magenta";
            case 4:
                return "yellow";
            case 5:
                return "green";
        }
        return "black";
    }

    /**
     * Filter to create a Cyan - colored version, based on GrayFilter
     */
    private class CyanFilter extends RGBImageFilter
    {
        public CyanFilter()
        {
            canFilterIndexColorModel = true;
        }

        boolean brighter = true;
        int percent = 40;

        // use gray filter, but modify red to be smaller component
        // Full cyan is 0,255,255 and the G/B components typically
        // stay the same
        public int filterRGB(int x, int y, int rgb)
        {
            // Use NTSC conversion formula.
            int gray = (int) ((0.30 * ((rgb >> 16) & 0xff) +
                               0.59 * ((rgb >> 8) & 0xff) +
                               0.11 * (rgb & 0xff)) / 3);

            if (brighter)
            {
                gray = (255 - ((255 - gray) * (100 - percent) / 100));
            }
            else
            {
                gray = (gray * (100 - percent) / 100);
            }

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            return (rgb & 0xff000000) | ((gray / 5) << 16) | (gray << 8) | (gray << 0);
        }
    }


    ////
    //// Icon methods
    ////

    private double dIconScale_ = 1.0d;

    /**
     * Return scale factor so icon fits in given width and height
     */
    public double getIconScaleFactor(int width, int height)
    {
        // getting min keeps figure entirely in area
        // getting max scales image as big as it can go (so one dimension equals width/height)
        return Math.min((double) width / (double) dInitialSize_.width,
                        (double) height / (double) dInitialSize_.height);
    }

    /**
     * This ImageComponent can be used as an
     * icon (e.g., in a button) at the same
     * time it is used elsewhere.  Use this
     * to set the size that the icon is drawn,
     * which is seperate from the size this image
     * is drawn.  The default scale is 1.0.  The
     * iconScale is multiplied by the width and
     * height of the ImageComponent, which is defined
     * by the original image size and the scale passed
     * in at construction time.  Clears icon height/width
     * if previously set.
     */
    public void setIconScale(double dScale)
    {
        dIconHeight_ = null;
        dIconWidth_ = null;
        dIconScale_ = dScale;
    }

    /**
     * Get icon scale factor
     */
    public double getIconScale()
    {
        return dIconScale_;
    }

    Integer dIconHeight_ = null;
    Integer dIconWidth_ = null;
    int nVerticalAlignment_ = SwingConstants.CENTER;
    int nHorizontalAlignment_ = SwingConstants.CENTER;

    /**
     * Set how to align icon if gap in space
     */
    public void setIconVerticalAlignement(int n)
    {
        nVerticalAlignment_ = n;
    }

    /**
     * Set how to align icon if gap in space
     */
    public void setIconHorizontalAlignement(int n)
    {
        nHorizontalAlignment_ = n;
    }

    /**
     * Override default icon height (doens't change icon
     * size, but space available to draw icon)
     */
    public void setIconHeight(Integer height)
    {
        dIconHeight_ = height;
    }

    /**
     * Override default icon width (doens't change icon
     * size, but space available to draw icon)
     */
    public void setIconWidth(Integer width)
    {
        dIconWidth_ = width;
    }

    /**
     * Returns the icon's height.
     *
     * @return an int specifying the fixed height of the icon.
     */
    public int getIconHeight()
    {
        if (dIconHeight_ != null) return dIconHeight_.intValue();
        return getActualIconHeight();
    }

    /**
     * Actual height of icon as its drawn
     */
    private int getActualIconHeight()
    {
        return (int) (dInitialSize_.height * dIconScale_);
    }

    /**
     * Returns the icon's width.
     *
     * @return an int specifying the fixed width of the icon.
     */
    public int getIconWidth()
    {
        if (dIconWidth_ != null) return dIconWidth_.intValue();
        return getActualIconWidth();
    }

    /**
     * Actual height of icon as its drawn
     */
    private int getActualIconWidth()
    {
        return (int) (dInitialSize_.width * dIconScale_);
    }

    /**
     * Draw the icon at the specified location.  Icon implementations
     * may use the Component argument to get properties useful for
     * painting, e.g. the foreground or background color.
     */
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        int iconwidth = getActualIconWidth();
        int iconheight = getActualIconHeight();
        int width = getIconWidth();
        int height = getIconHeight();

        paintIcon(g, x, y, width, height, iconwidth, iconheight,
                  nVerticalAlignment_, nHorizontalAlignment_, null);
    }

    /**
     * Actually paint icon
     */
    private void paintIcon(Graphics g, int x, int y, int width, int height,
                           int iconwidth, int iconheight,
                           int nVertAlign, int nHorizAlign,
                           Image image)
    {

        if (width > iconwidth)
        {
            switch (nHorizAlign)
            {
                case SwingConstants.CENTER:
                    x += (width - iconwidth) / 2;
                    break;

                case SwingConstants.LEFT:
                    break;

                case SwingConstants.RIGHT:
                    x += (width - iconwidth);
                    break;
            }
        }

        if (height > iconheight)
        {
            switch (nVertAlign)
            {
                case SwingConstants.CENTER:
                    y += (height - iconheight) / 2;
                    break;

                case SwingConstants.TOP:
                    break;

                case SwingConstants.BOTTOM:
                    y += (height - iconheight);
                    break;
            }
        }

        // if no image, just have image drawn as default (current state
        // of this image component
        if (image == null)
        {
            drawImageAt((Graphics2D) g, x, y, iconwidth, iconheight);
        }
        else
        {
            drawImageAt((Graphics2D) g, image, x, y, iconwidth, iconheight);
        }
    }

    /**
     * Returns a lightweight object that implements Icon, using this
     * and the current icon settings and image from this as its settings.
     * Useful when same image needs to represent two different sized icons
     * at same time.
     */
    public Icon getUniqueIcon()
    {
        return getUniqueIcon(false);
    }

    /**
     * Return icon - if bDisabled true, use disabled version of icon
     */
    public Icon getUniqueIcon(boolean bDisabled)
    {
        Image image = null;
        if (bDisabled)
        {
            image = getDisabledImage();
        }
        return new UniqueIcon(this, getIconWidth(), getIconHeight(),
                              getActualIconWidth(), getActualIconHeight(),
                              nVerticalAlignment_, nHorizontalAlignment_,
                              image);
    }

    /**
     * Class representing icon drawn using this ImageComponent
     */
    private class UniqueIcon implements Icon
    {
        ImageComponent ic;
        Image image;
        int width, height, iconwidth, iconheight, nVertAlign, nHorizAlign;

        UniqueIcon(ImageComponent ic, int width, int height,
                   int iconwidth, int iconheight,
                   int nVertAlign, int nHorizAlign,
                   Image image)
        {
            this.ic = ic;
            this.width = width;
            this.height = height;
            this.iconwidth = iconwidth;
            this.iconheight = iconheight;
            this.nVertAlign = nVertAlign;
            this.nHorizAlign = nHorizAlign;
            this.image = image;
        }

        public int getIconHeight()
        {
            return height;
        }

        public int getIconWidth()
        {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            ic.paintIcon(g, x, y, width, height, iconwidth, iconheight,
                         nVertAlign, nHorizAlign, image);
        }
    }

    public static interface CustomImage
    {
        public void paintCustom(Graphics2D g,
                                int x, int y, int width, int height);
    }
}
