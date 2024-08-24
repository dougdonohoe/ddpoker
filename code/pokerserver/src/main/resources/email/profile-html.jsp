<%@ page import="com.donohoedigital.base.*,
                 com.donohoedigital.games.poker.model.*,
                 com.donohoedigital.jsp.*" %>
<%
    String sName = (String) session.getAttribute(OnlineProfile.PROFILE_NAME);
    String sPassword = (String) session.getAttribute(OnlineProfile.PROFILE_PASSWORD);
    String sSubject = "DD Poker Online Profile - " + sName;
    session.setAttribute(JspEmail.PARAM_SUBJECT, sSubject);

%>
<HTML>
<BODY>
<TABLE WIDTH="600" BORDER="0">
    <TR>
        <TD>
            The following password has been assigned to your online profile &quot;<%= sName %>&quot;:
            <UL>
                <TT><font color="blue" size="+2"><%= Utils.encodeHTML(sPassword) %>
                </font></TT>
            </UL>
            <P>
                Please follow these steps to activate your profile:
            <OL>
                <LI>Open the DD Poker &quot;Player Profile&quot; screen.</LI>
                <LI>Select the &quot;<%= sName %>&quot; profile and click the &quot;Edit&quot; button.</LI>
                <LI>Enter the assigned password and click the &quot;OK&quot; button.</LI>
                <LI>You may change the password to something else by clicking the "Edit" button
                    then the "Change Password" button.</LI>
            </OL>
            Thanks again and enjoy the game!<BR>
            <BR>
            - DD Poker Team
        </TD>
    </TR>
</TABLE>
</BODY>
</HTML>
