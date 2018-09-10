package com.thinking.machines.tmws;
import javax.servlet.http.*;
public interface SessionAware
{
public void setHttpSession(HttpSession httpSession);
}