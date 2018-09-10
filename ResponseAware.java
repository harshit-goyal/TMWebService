package com.thinking.machines.tmws;
import javax.servlet.http.*;
public interface ResponseAware
{
public void setHttpServletResponse(HttpServletResponse httpServletResponse);
}