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
 * ImageDef.java
 *
 * Created on October 11, 2002, 6:02 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import javax.imageio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author  donohoe
 */
public class ImageDef 
{
    static Logger logger = LogManager.getLogger(ImageDef.class);

    private static boolean DEBUG = false;

    private String sName_;
    private URL url_;
    private ImageIcon icon_;
    private AnimatedImageIcon anim_;
    private BufferedImage bimage_;
    private boolean bCache_;
    private boolean bComposite_;
    private String[] saComponents_;
    private int x_ = 0;
    private int y_ = 0;

    // not sure if this is needed....
//    private static GraphicsConfiguration gc;
//
//    static {
//        gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
//    }
    
    /**
     * New image definition from name and its file
     */
    public ImageDef(String sName, URL url, Integer x, Integer y, Boolean bCache, boolean bComposite, String sComponents)
    {
        sName_ = sName;
        url_ = url;
        x_ = (x == null ? 0 : x);
        y_ = (y == null ? 0 : y);
        bCache_ = (bCache == null ? true : bCache);
        bComposite_ = bComposite;
        if (sComponents != null)
        {
            List<String> a = new ArrayList<String>();
            StringTokenizer tok = new StringTokenizer(sComponents, " ,");
            while (tok.hasMoreTokens())
            {
                a.add(tok.nextToken());
            }

            saComponents_ = new String[a.size()];
            for (int i = 0; i < saComponents_.length; i++)
            {
                saComponents_[i] = a.get(i);
            }
        }
    }
    
    /**
     * get file that this image resides in
     */
    public URL getImageURL()
    {
        return url_;
    }
    
    /**
     * get name of this image
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Get x hot spot for cursor
     */
    public int getX()
    {
        return x_;
    }
    
    /**
     * Get y hot spot for cursor
     */
    public int getY()
    {
        return y_;
    }

    /**
     * is Composite?
     */
    public boolean isComposite()
    {
        return bComposite_;
    }

    /**
     * get composite list
     */
    public String[] getComponents()
    {
        return saComponents_;
    }

    /**
     * Remove any cached images
     */
    public void clearCache()
    {
        anim_ = null;
        icon_ = null;
        bimage_ = null;
    }

    /**
     * Return ImageIcon file for requested image - source should
     * be an animated gif (different API because we need to use
     * a different loader).
     */
    public AnimatedImageIcon getAnimatedImageIcon()
    {
        if (anim_ == null)
        {
            AnimatedImageIcon anim = new AnimatedImageIcon(url_);
            if (!bCache_)
            {
                return anim;
            }
            anim_ = anim;
        }
        return anim_;
    }
    
    /**
     * Get image icon (wrapped around buffered image)
     */
    public ImageIcon getImageIcon()
    {
        if (icon_ == null)
        {
            BufferedImage bimage = getBufferedImage();
            if (bimage != null)
            {
                ImageIcon icon = new ImageIcon(bimage);
                if (!bCache_)
                {
                    return icon;
                }
                icon_ = icon;
            }
        }
        
        return icon_;
    }
    
    /**
     * Get buffered image for this image
     */
    public BufferedImage getBufferedImage()
    {
        if (bimage_ == null)
        {
            BufferedImage bimage = getBufferedImage(url_, bCache_);
            if (!bCache_)
            {
                return bimage;
            }
            bimage_ = bimage;
        }
        return bimage_;
    }


    /**
     * Get bufferred image from a file
     */
    public static BufferedImage getBufferedImage(File file)
    {
        URL url = null;
        try
        {
            url = new URL("file:"+file.getAbsolutePath());
        }
        catch (MalformedURLException e)
        {
            throw new ApplicationError(e);
        }
        return getBufferedImage(url, false);
    }

    /**
     * Get bufferred image from a url
     */
    public static BufferedImage getBufferedImage(URL url)
    {
        return getBufferedImage(url, false);
    }

    /**
     * internal code - pass whether image is cached, for debugging
     */
    private static BufferedImage getBufferedImage(URL url, boolean bCache)
    {
        try {
            BufferedImage src;

            // Apple broke PNG loading with ImageIO on 10.4
            if (Utils.ISMAC_10_4 && url.toString().toLowerCase().endsWith(".png"))
            {
                Image img = Toolkit.getDefaultToolkit().createImage(url);
                while (img.getHeight(null) == -1 || img.getWidth(null) == -1) Utils.sleepMillis(5);

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gs = ge.getDefaultScreenDevice();
                src = gs.getDefaultConfiguration().createCompatibleImage(
                        img.getWidth(null), img.getHeight(null), Transparency.TRANSLUCENT);
                Graphics g = src.createGraphics();
                while (!g.drawImage(img, 0, 0, null))
                {
                    Utils.sleepMillis(5);
                }
                g.dispose();
                img.flush();
            }
            else
            {
                src = ImageIO.read(url);
            }
            if (DEBUG) logSize(url.toString(), src, bCache);
            return src;
        }
        catch (Throwable e)
        {
            logger.error("Error creating buffered image from " + url);
            logger.error(Utils.formatExceptionText(e));
        }
        return null;
    }

    /**
     * get new buffered image
     */
    public static BufferedImage createBufferedImage(int w, int h, int type)
    {
        BufferedImage buf = new BufferedImage(w, h, type);
        if (DEBUG) logSize(" **NEW** "+w+"x"+h+" image", buf, false);
        return buf;
    }

    /**
     * get new buffered image
     */
    public static BufferedImage createBufferedImage(int w, int h, int type, IndexColorModel icm)
    {
        BufferedImage buf = new BufferedImage(w, h, type, icm);
        if (DEBUG) logSize(" **NEW ICM*** "+w+"x"+h+" image", buf, false);
        return buf;
    }

    private static long totalSize_ = 0;
    private static long totalSizeCached_ = 0;
    /**
     * Log size for debugging
     */
    private static  void logSize(String sName, BufferedImage src, boolean bCache)
    {
        long size = getImageSize(src);
        totalSize_ += size;
        if (bCache) totalSizeCached_ += size;
        logger.debug(PropertyConfig.getMessage("msg.imagesize.debug",
                                               sName,
                                               size,
                                               totalSizeCached_,
                                               totalSize_,
                                               bCache ? " (cached)" : " (not cached)"));
    }

    /**
     * Return memory used by image
     */
    public static long getImageSize(BufferedImage image)
    {
        DataBuffer db = image.getRaster().getDataBuffer();
        int dataType = db.getDataType();
        int elemenSizeInBits = DataBuffer.getDataTypeSize(dataType);
        return db.getNumBanks() * db.getSize() * elemenSizeInBits/ 8;
    }
}
    
