<%@ page import="com.donohoedigital.games.poker.model.*" %>
<%
    String sName = (String) session.getAttribute(OnlineProfile.PROFILE_NAME);
    String sPassword = (String) session.getAttribute(OnlineProfile.PROFILE_PASSWORD);

%>The following password has been assigned to your online profile "<%= sName %>":

<%= sPassword %>

Please follow these steps to activate your profile:

1. Open the DD Poker "Player Profile" screen.
2. Select the "<%= sName %>" profile and click the "Edit" button.
3. Enter the assigned password and click the "OK" button.
4. You may change the password to something else by clicking the "Edit" button
   then the "Change Password" button.

Thanks again and enjoy the game!

- DD Poker Team