<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%
String result;
try
{
String countS=request.getParameter("COUNT");
String Lat1S=request.getParameter("Lat1");
String Lat2S=request.getParameter("Lat2");
String Lng1S=request.getParameter("Lng1");
String Lng2S=request.getParameter("Lng2");
result=Generate.GenCity(Lat1S,Lng1S,Lat2S,Lng2S,countS);
} catch (Exception e)
{
result=e.toString();
}

%>

<%=result%>