/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/LayoutListener.java,v $
 * $Revision: 1.3 $
 * $Date: 2003/04/27 21:34:20 $
 *
 * Copyright (c) 2002 Zooki Technologies. All rights reserved.
 *
 * http://www.zookitec.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 *  Bug fixes, suggestions and comments should be sent to: alex@zookitec.com
 */
 
package com.zookitec.layout;

import java.io.Serializable;

/**
 * Interface for classes that can be notified each time ExplicitLayout lays out its container.
 * 
 *
 * <P>For example, a LayoutListener can be used to modify the layout constraints depending on the
 * container's width / height ratio.</P>
 *
 */
public interface LayoutListener extends Serializable {
    /**
     * Called before ExplicitLayout lays out its container.
     */
    public void beforeLayout(ExplicitLayout layout);
}
