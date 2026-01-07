<!--
  =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  DD Poker - Source Code
  Copyright (c) 2003-2026 Doug Donohoe
  
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  For the full License text, please see the LICENSE.txt file
  in the root directory of this project.
  
  The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
  graphics, text, and documentation found in this repository (including but not
  limited to written documentation, website content, and marketing materials) 
  are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
  4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
  without explicit written permission for any uses not covered by this License.
  For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
  in the root directory of this project.
  
  For inquiries regarding commercial licensing of this source code or 
  the use of names, logos, images, text, or other assets, please contact 
  doug [at] donohoe [dot] info.
  =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
-->
<%@ page import="
    com.donohoedigital.config.*,
                 com.donohoedigital.games.server.*,
                 com.donohoedigital.games.server.model.util.*,
                 com.donohoedigital.jsp.*"%>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%
    RegAnalyzer rega = (RegAnalyzer) session.getAttribute(JspFile.PARAM_CALLER);
    int nMax = 10; // max lines to output
    Date dnow = new Date(System.currentTimeMillis());
    String now = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss", Locale.US).format(dnow);
    int DISPLAY = 35;
    int WIDTH = 15;
    Calendar c;
%><HTML>
<TITLE>Registration report for <%= rega.getGame() %></TITLE>
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<BODY>
<CENTER>
<FONT COLOR="blue" SIZE=+1><B>Registration logs for <%= rega.getGame() %>
    generated <font color="red" size=+1><TT><%= now %></TT></font></B> (server time)</FONT>
    <% if (rega.getKeyStart() != null)
    {
    %>
    <br><i>(for keys starting with '<%= rega.getKeyStart()%>')</i>
    <%
    }
    %>

</CENTER>
<P>
<B>STATS</B>
<UL>
<P>
    <TABLE CELLPADDING=0>
    <TR><TD>Total Keys in Use&nbsp;&nbsp;&nbsp;</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getTotalRegistrations()) %></font></TD></TR>
    <TR><TD>Registrations</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getNumRegistrations()) %></font></TD></TR>
    <TR><TD>Activations</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getNumActivations()) %></font></TD></TR>
    <TR><TD>Patch Activations&nbsp;&nbsp;&nbsp;</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getNumPatches()) %></font></TD></TR>
    <TR><TD>Windows</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getNumWindows()) %></font></TD></TR>
    <TR><TD>Mac</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getNumMac()) %></font></TD></TR>
    <TR><TD>Linux</TD>
    <TD ALIGN=RIGHT><font color="blue"><%= PropertyConfig.getMessage("msg.integer", rega.getNumLinux()) %></font></TD></TR>
    </TABLE>
    
</UL>
<P>
&nbsp;<BR>
<B>DAILY COUNT - LAST <%= DISPLAY %> DAYS</B>
<UL>
    <TABLE> 
    <TR VALIGN=BOTTOM ALIGN=CENTER>
    <%
        RegAnalyzer.Counter counter;
        List<RegAnalyzer.Counter> daycnts = rega.getDayCount();
        int nStart = daycnts.size() - DISPLAY;
        if (nStart < 0) nStart = 0;
        int height;
        int cnt;
        int max = 0;

        // get max count
        for (int d = nStart; d < daycnts.size(); d++)
        {
            counter = daycnts.get(d);
            cnt = counter.getCount();
            if (cnt > max) max = cnt;
        }
        
        String daycolor;
        String dayname;

        for (int d = nStart; d < daycnts.size(); d++)
        {
            counter = daycnts.get(d);
            cnt = counter.getCount();
            height = max == 0 ? 1 : (cnt * 100) / max;
            dayname = counter.getComment();
            if (dayname.startsWith("S")) daycolor = "red";
            else daycolor = "blue";
    %>
        <TD><font size=-2><%= cnt %></font><BR><table width="<%= WIDTH %>" height=<%= height %> bgcolor=<%= daycolor %>><tr><td/></tr></table></TD>
    <%
        }
    %>
        </TR><TR ALIGN=CENTER>
    <%
        
        for (int d = nStart; d < daycnts.size(); d++)
        {
            counter = daycnts.get(d);
            dayname = counter.getComment();
            if (dayname.startsWith("S")) daycolor = "red";
            else daycolor = "black";
            c = new GregorianCalendar();
            c.set(Calendar.YEAR, counter.getYear());
            c.set(Calendar.DAY_OF_YEAR, counter.getSub());
            String sDate = ""+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.DAY_OF_MONTH);
    %>
    
        <TD ALIGN=CENTER><font color="<%= daycolor %>" size=-2><%= sDate %><BR><%= dayname %></font></TD>
    <%  
        }
    %>
    </TR>
    </TABLE>

</UL>
&nbsp;<BR>
<TABLE><TR><TD>
<B>REGISTRATIONS BY HOUR</B>
<UL>
    <TABLE> 
    <TR VALIGN=BOTTOM ALIGN=CENTER>
    <%
        int[] hourcnts = rega.getHourCount();
        int avg;
        int total = 0;
        max = 0;

        // get max count
        for (int hourcnt1 : hourcnts)
        {
            cnt = hourcnt1;
            if (cnt > max) max = cnt;
            total += cnt;
        }
        
        int running = 0;

        for (int hourcnt : hourcnts)
        {
            cnt = hourcnt;
            running += cnt;
            height = max == 0 ? 0 : (cnt * 100) / max;
            avg = total == 0 ? 0 : (100 * running) / total;
    %>
        <TD><font size=-2><%= cnt %>
        </font><BR>
            <font color="darkgreen" size=-2><%= avg %>%</font><BR>
            <table width="<%= WIDTH %>" height=<%= height %> bgcolor=blue>
                <tr>
                    <td/>
                </tr>
            </table>
        </TD>
        <%
            }
    %>
        </TR><TR ALIGN=CENTER>
    <%
        for (int e = 0; e < hourcnts.length; e++)
        {
    %>
    
        <TD><font size=-2><%= e %></font></TD>
    <%  
        }
    %>
    </TR>
    </TABLE>

</UL>
</TD><TD>
<B>REGISTRATIONS BY MONTH</B>
<UL>
    <TABLE> 
    <TR VALIGN=BOTTOM ALIGN=CENTER>
    <%
        List<RegAnalyzer.Counter> monthcnts = rega.getMonthCount();
        nStart = monthcnts.size() - 12;
        if (nStart < 0) nStart = 0;
        total = 0;
        max = 0;

        // get max count
        for (int e = nStart; e < monthcnts.size(); e++)
        {
            counter = monthcnts.get(e);
            cnt = counter.getCount();
            if (cnt > max) max = cnt;
            total += cnt;
        }

        int prev = 0;
        
        for (int e = nStart; e < monthcnts.size(); e++)
        {
            counter = monthcnts.get(e);
            cnt = counter.getCount();
            if (cnt == 0) continue;
            height = max == 0 ? 0 : (cnt * 100) / max;
            avg = prev == 0 ? 0 : ((cnt - prev) * 100 / prev);
            prev = cnt;
    %>
        <TD><font size=-2><%= cnt %></font><BR>
            <font color="darkgreen" size=-2><%= avg > 0 ? "+":""%><%= avg %>%</font><BR>
            <table width="<%= WIDTH %>" height=<%= height %> bgcolor=blue><tr><td/></tr></table></TD>
    <%
        }
    %>
        </TR><TR ALIGN=CENTER>
    <%
        for (int e = nStart; e < monthcnts.size(); e++)
        {
            counter = monthcnts.get(e);
            cnt = counter.getCount();
            if (cnt == 0) continue;
    %>
    
        <TD><font size=-2><%= counter.getSub() + 1 %></font></TD>
    <%  
        }
    %>
    </TR>
    </TABLE>

</UL>
</TD></TR></TABLE>
<B>REGISTRATIONS BY WEEK - LAST <%= DISPLAY %> WEEKS</B>
<UL>
    <TABLE> 
    <TR VALIGN=BOTTOM ALIGN=CENTER>
    <%
        List<RegAnalyzer.Counter> weekcnts = rega.getWeekCount();
        nStart = weekcnts.size() - DISPLAY;
        if (nStart < 0) nStart = 0;
        total = 0;
        max = 0;

        // get max count
        for (int e = nStart; e < weekcnts.size(); e++)
        {
            counter = weekcnts.get(e);
            cnt = counter.getCount();
            if (cnt > max) max = cnt;
            total += cnt;
        }

        prev = 0;
        for (int e = nStart; e < weekcnts.size(); e++)
        {
            counter = weekcnts.get(e);
            cnt = counter.getCount();
            if (cnt == 0) continue;
            height = (cnt * 100) / max;
            avg = prev == 0 ? 0 : ((cnt - prev) * 100 / prev);
            prev = cnt;
    %>
        <TD><font size=-2><%= cnt %></font><BR>
            <font color="darkgreen" size=-2><%= avg > 0 ? "+":""%><%= avg %>%</font><BR>
            <table width="<%= WIDTH %>" height=<%= height %> bgcolor=blue><tr><td/></tr></table></TD>
    <%
        }
    %>
        </TR><TR ALIGN=CENTER>
    <%
        for (int e = nStart; e < weekcnts.size(); e++)
        {
            counter = weekcnts.get(e);
            cnt = counter.getCount();
            if (cnt == 0) continue;
    %>
    
        <TD><font size=-2><%= (counter.getComment()) %></font></TD>
    <%  
        }
    %>
    </TR>
    </TABLE>

</UL>
<P>
<B>SUSPECT KEYS</B>
<UL>
<P>
<TABLE BORDER=1 CELLPADDING=5>
<%
    List<RegInfo> all = rega.getSuspectKeys();
    int nCnt = 0;
    for (RegInfo info : all)
    {
        if (info.getNumTotal() == 0)
        {
            // Test keys (aka, Doug and Greg).
            continue;
        }

        nCnt++;
%>
    <TR bgcolor=white>
        <TD WIDTH=180 VALIGN=TOP>
            <B><%= nCnt %>.</B>&nbsp;&nbsp;&nbsp;<font color="blue"><%= info.getNumTotal() %> entries</font><BR>
            <font color="red"><%= info.getKey() %>
            </font><%= info.isDownload() ? " (d)" : "" %><BR>
        </TD>
        <TD VALIGN=TOP>
            <% if (info.getNumRegistrations() > 0)
            { %>
            <font color="blue">Registrations (<%= info.getNumRegistrations() %>)</font>
            <UL>
                <%= info.toHTML(info.getRegistrations(), nMax) %>
            </UL>
            <% } %>
            <% if (info.getNumActivations() > 0)
            { %>
            <font color="blue">Activations (<%= info.getNumActivations() %>)</font>
            <UL>
                <%= info.toHTML(info.getActivations(), nMax) %>
            </UL>
            <% } %>

            <% if (info.getNumPatches() > 0)
            { %>
            <font color="blue">Patches (<%= info.getNumPatches() %>)</font>
            <UL>
                <%= info.toHTML(info.getPatches(), nMax) %>
            </UL>
            <% } %>

            <% if (info.getNumDups() > 0)
            { %>
            <font color="blue">Duplicates (<%= info.getNumDups() %>)</font>
            <UL>
                <%= info.toHTML(info.getDups(), nMax) %>
            </UL>
            <% } %>

        </TD>
    </TR>
    <%
        }
%>
</TABLE>
</UL>
<P>
<B>BANNED KEYS</B>
<UL>
<P>
<TABLE BORDER=1 CELLPADDING=5>
<%
    all = rega.getBannedKeys();
    nCnt = 0;
    for (RegInfo info : all)
    {
        nCnt++;
%>
    <TR bgcolor=FFFF99>
        <TD WIDTH=180 VALIGN=TOP>
            <B><%= nCnt %>.</B>&nbsp;&nbsp;&nbsp;<font color="blue"><%= info.getNumTotal() %> entries</font><BR>
            <font color="red"><%= info.getKey() %>
            </font><%= info.isDownload() ? " (d)" : "" %><BR><BR>
            <i><%= info.getBannedComment() %></i>
        </TD>
        <TD VALIGN=TOP>
            <% if (info.getNumRegistrations() > 0)
            { %>
            <font color="blue">Registrations (<%= info.getNumRegistrations() %>)</font>
            <UL>
                <%= info.toHTML(info.getRegistrations(), nMax) %>
            </UL>
            <% } %>
            <% if (info.getNumActivations() > 0)
            { %>
            <font color="blue">Activations (<%= info.getNumActivations() %>)</font>
            <UL>
                <%= info.toHTML(info.getActivations(), nMax) %>
            </UL>
            <% } %>

            <% if (info.getNumPatches() > 0)
            { %>
            <font color="blue">Patches (<%= info.getNumPatches() %>)</font>
            <UL>
                <%= info.toHTML(info.getPatches(), nMax) %>
            </UL>
            <% } %>

            <% if (info.getNumDups() > 0)
            { %>
            <font color="blue">Duplicates (<%= info.getNumDups() %>)</font>
            <UL>
                <%= info.toHTML(info.getDups(), nMax) %>
            </UL>
            <% } %>

        </TD>
    </TR>
    <%
        }
%>
</TABLE>
</UL>
</BODY></HTML>
