package com.thinking.machines.tmws;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
public class ConfigurationReader
{
public static ArrayList<String> getPackages(File file) throws ConfigurationException
{
ArrayList<String> packages=new ArrayList<String>();
try
{ if(file.exists()==false)
{ throw new ConfigurationException("Configuration file :"+file.getName()+" is missing");
}
DocumentBuilderFactory documentBuilderFactory;
documentBuilderFactory=DocumentBuilderFactory.newInstance();
DocumentBuilder documentBuilder;
documentBuilder=documentBuilderFactory.newDocumentBuilder();
Document document;
document=documentBuilder.parse(file);
String rootNodeName=document.getDocumentElement().getNodeName();
if(rootNodeName.equals("packages")==false)
{ throw new ConfigurationException("<packages> if not the root element of the configuration file.");
}
NodeList rootNodeList;
rootNodeList=document.getElementsByTagName("packages");
if(rootNodeList.getLength()>1)
{
throw new ConfigurationException("Multiple times <packages> found in the configuration file.");
}
Node rootNode;
NodeList listOfPackages;
Node childNode;
Element element;
String tagName;
String tagContent;
rootNode=rootNodeList.item(0);
listOfPackages=rootNode.getChildNodes();
for(int j=0;j<listOfPackages.getLength();j++)
{
childNode=listOfPackages.item(j);
tagName=childNode.getNodeName();
if(tagName.equals("#text")) continue;
if(tagName.equals("package")==false)
{
throw new ConfigurationException("<packages> does not contain <=package> at number :"+(j+1));
} tagContent=childNode.getTextContent();
packages.add(tagContent.replaceAll("\r","").replaceAll("\n",""));
}
}
catch(Exception exception)
{
ConfigurationException configurationException=new
ConfigurationException(exception.getMessage());
throw configurationException;
}
return packages;
}
}