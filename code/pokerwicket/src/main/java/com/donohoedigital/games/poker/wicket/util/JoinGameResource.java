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
package com.donohoedigital.games.poker.wicket.util;


import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.OnlineGame;
import org.apache.wicket.request.resource.ByteArrayResource;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 4, 2008
 * Time: 3:01:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class JoinGameResource
{
	public static ByteArrayResource create(OnlineGame game, boolean observe) {
		String name = "game" + game.getId() + (observe ? "obs" : "") + '.' +
                PokerConstants.JOIN_FILE_EXT;
		StringBuilder sb = new StringBuilder(game.getUrl());
		if (observe) sb.append(PokerConstants.JOIN_OBSERVER_QUERY);
		sb.append('\n');
		byte[] url = Utils.encodeBasic(sb.toString());
		return new ByteArrayResource(PokerConstants.CONTENT_TYPE_JOIN, url, name);
	}
}
