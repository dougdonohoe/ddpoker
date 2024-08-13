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
 *
 */

package com.donohoedigital.mail;

import com.donohoedigital.base.*;

import javax.activation.*;
import java.io.*;

public class DDAttachment implements DataSource
{
	byte[] data_;
    String sContentType_;
    String sFileName_;

    public DDAttachment(String sFileName, String sData, String sContentType)
    {
        this(sFileName, Utils.encode(sData), sContentType);
    }
    
    public DDAttachment(String sFileName, byte[] data, String sContentType)
    {
        data_ = data;
        sContentType_ = sContentType;
        sFileName_ = sFileName;
    }
    
    public String getFileName() {
        return sFileName_;
    }
    
    public String getContentType() {
        return sContentType_;
    }

    public java.io.InputStream getInputStream() throws java.io.IOException {
        return new ByteArrayInputStream(data_);
    }

    public String getName() {
        return "DDAttachment";
    }

    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        throw new IOException("Unsupported");
    }
}
