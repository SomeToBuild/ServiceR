<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.SpiritProto"%>
<%
String token=request.getParameter("Token");
String city=null;
if (request.getParameterMap().containsKey("City")) {
            city = request.getParameter("City");
        }
SpiritProto obj=new SpiritProto();
String result = obj.getRouteList(token,city);

%>
<%=result%>