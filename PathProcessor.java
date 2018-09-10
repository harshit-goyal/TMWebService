package com.thinking.machines.tmws;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import com.thinking.machines.tmws.annotations.*;
class PathProcessor
{
private String path;
private Class mappedClass;
private Method method;
private Class parameterTypes[];
private Object object=null;
private boolean isDirectoryAware;
private boolean isSessionAware;
private boolean isApplicationAware;
private boolean isRequestAware;
private boolean isResponseAware;
private boolean isFilesAware;
private boolean isReturningSomething;
private Data produces;
private boolean allowGetTypeRequest;
private boolean allowPostTypeRequest;
public PathProcessor(String path,Class mappedClass,Method method,boolean isReturningSomething,Data produces,boolean isDirectoryAware,boolean isSessionAware,boolean isApplicationAware,boolean isRequestAware,boolean isResponseAware,boolean isFilesAware,boolean allowGetTypeRequest,boolean allowPostTypeRequest)
{this.path=path;
this.allowGetTypeRequest=allowGetTypeRequest;
this.allowPostTypeRequest=allowPostTypeRequest;
this.method=method;
this.isReturningSomething=isReturningSomething;
this.produces=produces;
this.mappedClass=mappedClass;
this.parameterTypes=method.getParameterTypes();
this.isDirectoryAware=isDirectoryAware;
this.isSessionAware=isSessionAware;
this.isApplicationAware=isApplicationAware;
this.isRequestAware=isRequestAware;
this.isResponseAware=isResponseAware;
this.isFilesAware=isFilesAware;
}
public String getPath()
{
return this.path;
}
public Class getMappedClass()
{
return this.mappedClass;
}
public Method getMethod()
{
return this.method;
}
public Class[] getParameterTypes()
{
return this.parameterTypes;
}
public Object getObject()
{
if(isSessionAware && isDirectoryAware && isApplicationAware)
{
return newObject();
} if(object==null)
{ try
{
object=mappedClass.newInstance();
}catch(Exception exception)
{
}
}
return object;
}
private Object newObject()
{
Object object=null;
try
{
object=mappedClass.newInstance();
}catch(Exception exception)
{
}
return object;
}
public boolean isDirectoryAware()
{
return isDirectoryAware;
}
public boolean isSessionAware()
{
return isSessionAware;
}
public boolean isApplicationAware()
{
return isApplicationAware;
}
public boolean isRequestAware()
{
return isRequestAware;
}
public boolean isResponseAware()
{
return isResponseAware;
}
public boolean isFilesAware()
{
return isFilesAware;
}
public boolean isReturningSomething()
{
return this.isReturningSomething;
}
public Data produces()
{
return this.produces;
}
public void allowGetTypeRequest(boolean alloGetTypeRequest)
{
this.allowGetTypeRequest=allowGetTypeRequest;
}
public void allowPostTypeRequest(boolean alloPostTypeRequest)
{
this.allowPostTypeRequest=allowPostTypeRequest;
}
public boolean allowGetTypeRequest()
{
return this.allowGetTypeRequest;
}
public boolean allowPostTypeRequest()
{
return this.allowPostTypeRequest;
}
}