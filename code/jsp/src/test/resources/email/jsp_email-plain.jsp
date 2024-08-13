<%@ page import="com.donohoedigital.jsp.*" %><%
    String sName = (String) session.getAttribute("name");
    String sSubject = "Subject Test";
    session.setAttribute(JspEmail.PARAM_SUBJECT, sSubject);
%>PLAIN <%= sName %>