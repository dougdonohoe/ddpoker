<%@ page import="com.donohoedigital.comms.*,
                 com.donohoedigital.games.server.*,
                 com.donohoedigital.jsp.*" %>
<%

    String sName = (String) session.getAttribute(EngineServlet.EMAIL_PARAM_NAME);
    String sSubject = "DD Poker Registration";
    session.setAttribute(JspEmail.PARAM_SUBJECT, sSubject);
%>
<HTML>
<BODY>
<%= sName %>:<BR>
<BR>
Thank you for registering your copy of DD Poker.<BR>
<BR>
- DD Poker Team
</BODY>
</HTML>
