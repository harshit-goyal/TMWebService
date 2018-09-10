package com.thinking.machines.tmws;
import javax.servlet.http.*;
public interface RequestAware
{
public void setHttpServletRequest(HttpServletRequest httpServletRequest);
}