<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.SpiritProto"%>
<%
String Login=request.getParameter("Login");
String Password=request.getParameter("Password");

Client obj=new Client();
String token=obj.GetToken(Login, Password);
%>    
<%=token%>