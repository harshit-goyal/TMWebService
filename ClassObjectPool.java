package com.thinking.machines.tmws;
import java.lang.reflect.*;
import java.util.*;
public class ClassObjectPool
{
private static HashMap<String,Class> classes=new HashMap<String,Class>();
private static HashMap<String,Object> objects=new HashMap<String,Object>();
private ClassObjectPool(){}
public static Class getClassType(String className)
{
Class c=classes.get(className);
if(c==null)
{ try
{
c=Class.forName(className);
classes.put(className,c);
}catch(ClassNotFoundException classNotFoundException)
{
}
}
return c;
}
public static Object getObject(String className)
{
Object o=objects.get(className);
if(o==null)
{
try
{
Class c=getClassType(className);
o=c.newInstance();
objects.put(className,o);
}catch(Throwable t)
{
}
}
return o;
}
}