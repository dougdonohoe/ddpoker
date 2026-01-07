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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.db.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.service.*;
import org.apache.logging.log4j.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

import java.util.*;

/**
 * Command line tool to clean up old WAN games.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class OnlineProfilePurger extends BaseCommandLineApp
{
    private static Logger logger = LogManager.getLogger(OnlineProfilePurger.class);

    private OnlineProfileService service;

    /**
     * Run purger.
     */
    public static void main(String[] args)
    {
        try {
            new OnlineProfilePurger("poker", args);
        }
        catch (ApplicationError ae)
        {
            System.err.println("OnlineProfilePurger ending due to ApplicationError: " + ae.toString());
        }
        catch (Throwable t)
        {
            System.err.println(Utils.formatExceptionText(t));
        }

        System.exit(0);
    }

    /**
     * Create the purger instance.
     */
    public OnlineProfilePurger(String configName, String[] args)
    {
        super(configName, args);

        // get the service from spring
        ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-pokertools.xml");
        service = (OnlineProfileService) ctx.getBean("onlineProfileService");

        // Do the work.
        doPurge();
    }

    /**
     * Purge the records.
     */
    private void doPurge()
    {
        days_90 = Utils.getDateDays(-90);
        days_14 = Utils.getDateDays(-14);

        int offset = 0;
        int pagesize = 5000;
        int processed = 0;

        PagedList<OnlineProfilePurgeSummary> list = service.getOnlineProfilePurgeSummary(null, offset, pagesize);
        while (!list.isEmpty())
        {
            for (OnlineProfilePurgeSummary sum : list)
            {
                doPurge(sum);
                processed ++;
            }
            offset += pagesize;
            list = service.getOnlineProfilePurgeSummary(list.getTotalSize(), offset, pagesize);
        }

        ApplicationError.assertTrue(processed == list.getTotalSize(), "Processed " + processed + " of " + list.getTotalSize() + " rows");

        logger.debug("Deleting " + deleteList.size() + " total profiles");
        service.deleteOnlineProfiles(deleteList);
    }

    private List<OnlineProfile> deleteList = new ArrayList<OnlineProfile>();
    private OnlineProfilePurgeSummary last;
    private Date days_90;
    private Date days_14;

    // purge list is ordered by key and history count
    private void doPurge(OnlineProfilePurgeSummary sum)
    {
        OnlineProfile p = sum.getOnlineProfile();

        // don't process dummy profiles
        if (sum.getOnlineProfile().getLicenseKey().startsWith(PokerConstants.DUMMY_PROFILE_KEY_START))
        {
            return;
        }

        if (sum.getHistoryCount() == 0)
        {
            boolean delete = false;
            // if last profile we saw was same key, eliminate if not used in 14 days
            if (lastSameKey(sum))
            {
                if (p.getModifyDate().getTime() < days_14.getTime())
                {
                    logger.debug("oooo Deleting " + p.getName() + " since this person has other profiles and this one not used in 14 days");
                    delete = true;
                }
                else
                {
                    logger.debug(".... Keeping " + p.getName() + " because created in past 14 days (even though there are other profiles)");
                }
            }
            else if (p.getModifyDate().getTime() < days_90.getTime())
            {
                logger.debug("++++ Deleting " + p.getName() + " because not used in 90 days");
                delete = true;
            }
            else
            {
                logger.debug(".... Keeping " + p.getName() + " because created in past 90 days");
            }

            if (delete) deleteList.add(p);
        }

        // only remember last if it had histories
        if (sum.getHistoryCount() > 0)
        {
            last = sum;
        }
    }

    private boolean lastSameKey(OnlineProfilePurgeSummary sum)
    {
        return (last != null && last.getOnlineProfile().getLicenseKey().equals(sum.getOnlineProfile().getLicenseKey()));
    }
}