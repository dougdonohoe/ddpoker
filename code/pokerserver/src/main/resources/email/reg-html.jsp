<%@ page import="com.donohoedigital.comms.*,
                 com.donohoedigital.games.server.*,
                 com.donohoedigital.jsp.*" %>
<%

    String sName = (String) session.getAttribute(EngineServlet.EMAIL_PARAM_NAME);
    Version version = (Version) session.getAttribute(EngineServlet.EMAIL_PARAM_VERSION);
    String sSubject = "DD Poker Registration";
    session.setAttribute(JspEmail.PARAM_SUBJECT, sSubject);

%>
<HTML>
<BODY>
<TABLE WIDTH="600" BORDER="0">
    <TR>
        <TD>
            <B><FONT COLOR="blue" SIZE=+1><%= sName %>:</FONT></B>

            <P>
                Thank you for registering your copy of DD Poker.<BR>
                <BR>
                If you have any questions or issues playing DD Poker,
                you may look for answers at our support page:<BR>
            <UL>
                <a href="http://www.ddpoker.com/support">http://www.ddpoker.com/support</a>
            </UL>
            <%
                if (version.getMajor() != 3)
                {
            %>
            <BR>
            <center>
                <table width=450 border=1>
                    <tr>
                        <td>
                            <B><font color="red">NOTE</font></B>: Your version of DD Poker is <%= version.toString() %>.
                            DD Poker version 3 was released in early 2009. You may upgrade for free at:
                            <UL>
                                <a href="http://www.ddpoker.com/download">http://www.ddpoker.com/download</a>
                            </UL>
                        </td>
                    </tr>
                </table>
            </center>
            <BR>
            <%
                }
            %>
            You may also discuss DD Poker at our forums: <BR>
            <UL>
                <a href="http://www.ddpoker.com/forums">http://www.ddpoker.com/forums</a>
            </UL>
            Thanks again and enjoy the game!<BR>
            <BR>
            - Donohoe Digital
        </TD>
    </TR>
</TABLE>
</BODY>
</HTML>
