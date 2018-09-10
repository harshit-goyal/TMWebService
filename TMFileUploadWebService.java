package com.thinking.machines.tmws;
import java.util.zip.*;
import com.thinking.machines.tmws.annotations.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
@MultipartConfig
public class TMFileUploadWebService extends HttpServlet
{
public void doPost(HttpServletRequest request,HttpServletResponse response)
{
ServletContext servletContext=getServletContext();
HashMap<String,PathProcessor> paths=(HashMap<String,PathProcessor>)servletContext.getAttribute("paths");
RequestProcessor.process(getServletContext(),request,response,paths);
}
}