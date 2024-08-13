<%@ page import="com.donohoedigital.comms.*,
                 com.donohoedigital.games.server.*" %>
<%

    String sName = (String) session.getAttribute(EngineServlet.EMAIL_PARAM_NAME);
    Version version = (Version) session.getAttribute(EngineServlet.EMAIL_PARAM_VERSION);

%><%= sName %>:

Thank you for registering your copy of DD Poker.

If you have any questions or issues playing DD Poker,
you may look for answers at our support page:

http://www.ddpoker.com/support

<%
    if (version.getMajor() != 3)
    {
%>
Note: Your version of DD Poker is <%= version.toString() %>.
DD Poker version 2 was released in early 2009.
You may upgrade for free at:

http://www.ddpoker.com/download

<%
    }
%>
You may also discuss DD Poker at our forums:

http://www.ddpoker.com/forums

Thanks again and enjoy the game!

- Donohoe Digital
