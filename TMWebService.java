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
public class TMWebService extends HttpServlet
{
public void init()
{
ServletContext servletContext=getServletContext();
if(servletContext.getAttribute("developmentMode")==null)
{
String developmentMode=getInitParameter("development-mode");
if(developmentMode!=null && developmentMode.toUpperCase().equals("TRUE"))
{
servletContext.setAttribute("developmentMode",new Boolean(true));
}
else
{
servletContext.setAttribute("developmentMode",new Boolean(false));
}
}
if(servletContext.getAttribute("paths")==null)
{
List<String> listOfUserPackages=null;
try
{ listOfUserPackages=getListOfUserPackages();
}catch(ConfigurationException configurationException)
{
System.out.println("********************TMWS**********");
System.out.println(configurationException.getMessage());
System.out.println("********************TMWS**********");
return;
}
System.out.println("********************TMWS**********");
if(listOfUserPackages!=null)
{
for(int x=0;x<listOfUserPackages.size();x++)
{
System.out.println(listOfUserPackages.get(x));
}
System.out.println("********************TMWS**********");
List<String> errors=new ArrayList<String>();
HashSet<String> fullPackageNames=new HashSet<String>();
HashSet<String> partialPackageNames=new HashSet<String>();
String packageName;
for(int x=0;x<listOfUserPackages.size();x++)
{
packageName=listOfUserPackages.get(x);
if(packageName.endsWith(".*"))
{
partialPackageNames.add(packageName.substring(0,packageName.length()-1));
} else
{
fullPackageNames.add(packageName);
}
}
loadPaths(fullPackageNames,partialPackageNames,errors);
genrateJQueryJavaScriptFile();
} else
{
System.out.println("********************TMWS**********");
System.out.println("No paths loaded as packages not specified");
System.out.println("********************TMWS**********");
} //createWebServiceReport(errors);
}
}
public void doGet(HttpServletRequest request,HttpServletResponse response)
{
processRequest(request,response);
}
public void doPost(HttpServletRequest request,HttpServletResponse response)
{
processRequest(request,response);
}
public void processRequest(HttpServletRequest request,HttpServletResponse response)
{
ServletContext servletContext=getServletContext();
HashMap<String,PathProcessor> paths=(HashMap<String,PathProcessor>)servletContext.getAttribute("paths");
RequestProcessor.process(getServletContext(),request,response,paths);
}
private List<String> getListOfUserPackages() throws ConfigurationException
{
Enumeration<String> enumeration=getInitParameterNames();
ArrayList<String> paramNames=new ArrayList<String>();
while(enumeration.hasMoreElements())
{
paramNames.add(enumeration.nextElement());
} if(paramNames.size()==0)
{
return null;
} if((paramNames.contains("packages") && paramNames.size()>1) && paramNames.contains("development-mode")==false)
{
throw new ConfigurationException("if param with packages is supplied, then nothing else can be supplied as param name other than development-mode");
} if((paramNames.contains("configuration") && paramNames.size()>1) && paramNames.contains("development-mode")==false)
{
throw new ConfigurationException("if param with configuration is supplied, then nothing else can be supplied as param name other than development-mode");
}
if(paramNames.contains("packages")==false && paramNames.contains("configuration")==false)
{
boolean valid=true;
int packageNumber=1;
String message="";
while(packageNumber<=paramNames.size())
{ if(paramNames.contains("package-"+packageNumber)==false)
{ if(message.length()>0) message=message+",";
message=message+"package-"+packageNumber;
valid=false;
}
packageNumber++;
}
if(valid==false)
{
throw new ConfigurationException("Missing package information : "+message);
}
}
ArrayList<String> list=new ArrayList<String>();
String packageNames=getInitParameter("packages");
if(packageNames!=null && packageNames.trim().length()>0)
{
packageNames=packageNames.trim();
boolean containsComma,containsNewLine;
containsComma=packageNames.indexOf(",")!=-1;
containsNewLine=packageNames.indexOf("\n")!=-1;
if(containsComma==false && containsNewLine==false)
{
list.add(packageNames);
return list;
}
if(containsComma && containsNewLine)
{
packageNames=packageNames.replaceAll("\r","");
packageNames=packageNames.replaceAll("\n","");
containsNewLine=false;
} if(containsNewLine)
{
packageNames=packageNames.replaceAll("\r","");
packageNames=packageNames.replaceAll("\n",",");
containsNewLine=false;
}
String pcs[]=packageNames.split(",");
for(String pc:pcs)
{ if(pc.trim().length()>0)
{ list.add(pc.trim());
}}
return list;
} // packages hone wali if condition
String configurationFileName=getInitParameter("configuration");
if(configurationFileName!=null && configurationFileName.length()>0)
{ if(configurationFileName.startsWith("/")==false)
{ throw new ConfigurationException("Invalid cofiguration file path, must start with /");
}
String actualConfigurationFileName=getServletContext().getRealPath("/")+configurationFileName.substring(1);
File file=new File(actualConfigurationFileName);
return ConfigurationReader.getPackages(file);
}
String packageName=getInitParameter("package-1");
int x=2;
while(packageName!=null)
{ list.add(packageName);
packageName=getInitParameter("package-"+x);
x++;
}
return list;
}
private void loadPaths(HashSet<String> fullPackageNames,HashSet<String> partialPackageNames,List<String> errors)
{
ServletContext servletContext=getServletContext();
String baseFolder=servletContext.getRealPath("/");
String jarFolder=baseFolder+"WEB-INF/lib";
String classesFolder=baseFolder+"WEB-INF/classes";
// Logic to analyze and process starts here
ArrayList<File> directories=new ArrayList<File>();
ArrayList<File> jars=new ArrayList<File>();
directories.add(new File(classesFolder));
File jarFiles[]=new File(jarFolder).listFiles();
for(int xx=0;xx<jarFiles.length;xx++)
{if(jarFiles[xx].getName().endsWith(".jar"))
{
jars.add(jarFiles[xx]);
}}
File file;
System.out.println("Number of directories : "+directories.size());
System.out.println("Number of jars : "+jars.size());
HashMap<String,PathProcessor> pathProcessors=new HashMap<String,PathProcessor>();
//HashMap<String, done done
for(int e=0;e<directories.size();e++)
{
populatePathProcessorsFromDirectory(directories.get(e),pathProcessors,directories.get(e).getAbsolutePath(),fullPackageNames,partialPackageNames,errors);
}
populatePathProcessorsFromJars(jars,pathProcessors,fullPackageNames,partialPackageNames,errors);
// display results
Iterator iterator=pathProcessors.entrySet().iterator();
System.out.println(pathProcessors.size());
Map.Entry entry;
while(iterator.hasNext())
{ entry=(Map.Entry)iterator.next();
System.out.println("Key : "+entry.getKey());
System.out.println("Value : "+((PathProcessor)entry.getValue()).getMappedClass().getName());
}
// Logic to analyze and process ends here
servletContext.setAttribute("paths",pathProcessors);
}
public static void populatePathProcessorsFromDirectory(File directory,HashMap<String,PathProcessor> pathProcessors,String baseDirectory,HashSet<String> fullPackageNames,HashSet<String> partialPackageNames,List<String> errors)
{
String classFileName;
File []files;
files=directory.listFiles();
for(int e=0;e<files.length;e++)
{
if(files[e].isDirectory())
{
populatePathProcessorsFromDirectory(files[e],pathProcessors,baseDirectory,fullPackageNames,partialPackageNames,errors);
}
else
{
if(files[e].getName().endsWith(".class"))
{
classFileName=files[e].getAbsolutePath().substring(baseDirectory.length()+1).replaceAll("/",".").replaceAll("\\\\",".");
classFileName=classFileName.substring(0,classFileName.length()-6);
setPathProcessors(classFileName,pathProcessors,fullPackageNames,partialPackageNames,errors);
}
}
}
}
public static void populatePathProcessorsFromJars(ArrayList<File> jars,HashMap<String,PathProcessor> pathProcessors,HashSet<String> fullPackageNames,HashSet<String> partialPackageNames,List<String> errors)
{
ZipInputStream jarFile;
for(int e=0;e<jars.size();e++)
{ try
{
jarFile=new ZipInputStream(new FileInputStream(jars.get(e)));
ZipEntry entry=jarFile.getNextEntry();
String classFileName;
while(entry!=null)
{ if(entry.isDirectory()==false && entry.getName().endsWith(".class"))
{ classFileName=entry.getName();
classFileName=classFileName.substring(0,classFileName.length()-6).replaceAll("/",".").replaceAll("\\\\",".");
setPathProcessors(classFileName,pathProcessors,fullPackageNames,partialPackageNames,errors);
}
entry=jarFile.getNextEntry();
}
}
catch(Exception ee)
{
System.out.println("in jar's catch : "+ee);
}}}
static public void setPathProcessors(String className,HashMap<String,PathProcessor> pathProcessors,HashSet<String> fullPackageNames,HashSet<String> partialPackageNames,List<String> errors)
{
//System.out.println("Processing : "+className);
try
{
Class c=ClassObjectPool.getClassType(className);
if(c==null) return;
String packageName=c.getPackage().getName();
boolean consider=false;
if(fullPackageNames.contains(packageName))
{ consider=true;
} if(consider==false)
{
Iterator<String> iterator=partialPackageNames.iterator();
String value;
while(iterator.hasNext())
{
value=iterator.next();
if(packageName.startsWith(value))
{ consider=true;
break;
}
}
}
if(consider==false) return;
Annotation a=c.getAnnotation(Path.class);
if(a==null) return;
String pathToClass;
pathToClass=((Path)a).value();
if(pathToClass==null) return;
Method methods[]=c.getDeclaredMethods();
String pathToMethod;
String path;
for(int e=0;e<methods.length;e++)
{ a=methods[e].getAnnotation(Path.class);
if(a!=null)
{
pathToMethod=((Path)a).value();
if(pathToMethod!=null)
{
path=pathToClass+"/"+pathToMethod;
if(isValidPath(path))
{
if(pathProcessors.get(path)==null)
{
boolean isReturningSomething=methods[e].getReturnType().getSimpleName().equals("void")==false;
Annotation pa=methods[e].getAnnotation(Produces.class);
Data produces=Data.JSON;
if(pa!=null)
{
Produces p=(Produces)pa;
produces=p.value();
}
boolean getType=false;
boolean postType=false;
pa=methods[e].getAnnotation(Post.class);
if(pa!=null)
{
postType=true;
if(methods[e].getAnnotation(Get.class)!=null)
{
getType=true;
}
}else if(methods[e].getAnnotation(Get.class)!=null)
{
getType=true;
}
else
{
getType=true;
postType=true;
}
boolean isRequestAwareApplied;
isRequestAwareApplied=isRequestAware(c);
if(!isRequestAwareApplied)
{
isRequestAwareApplied=methods[e].getAnnotation(InjectRequest.class)!=null;
}
boolean isResponseAwareApplied;
isResponseAwareApplied=isResponseAware(c);
if(!isResponseAwareApplied)
{
isResponseAwareApplied=methods[e].getAnnotation(InjectResponse.class)!=null;
}
boolean isApplicationAwareApplied;
isApplicationAwareApplied=isApplicationAware(c);
if(!isApplicationAwareApplied)
{
isApplicationAwareApplied=methods[e].getAnnotation(InjectApplication.class)!=null;
}
boolean isSessionAwareApplied;
isSessionAwareApplied=isSessionAware(c);
if(!isSessionAwareApplied)
{
isSessionAwareApplied=methods[e].getAnnotation(InjectSession.class)!=null;
}
boolean isDirectoryAwareApplied;
isDirectoryAwareApplied=isDirectoryAware(c);
if(!isDirectoryAwareApplied)
{
isDirectoryAwareApplied=methods[e].getAnnotation(InjectDirectory.class)!=null;
}
boolean isFilesAwareApplied;
isFilesAwareApplied=isFilesAware(c);
if(!isFilesAwareApplied)
{
isFilesAwareApplied=methods[e].getAnnotation(InjectFiles.class)!=null;
}
if(isFilesAwareApplied) getType=false;
pathProcessors.put(path,new PathProcessor(path,c,methods[e],isReturningSomething,produces,isDirectoryAwareApplied,isSessionAwareApplied,isApplicationAwareApplied,isRequestAwareApplied,isResponseAwareApplied,isFilesAwareApplied,getType,postType));
} else
{ errors.add("Path :"+path+" already defined earlier and hence cannot be associated with method :"+methods[e].toString()+" of class : "+c.getName());
// a path processor exists against that path, it is multiple same path problem
}} else
{ errors.add("Invalid path :"+path+" against method :"+methods[e].toString()+" of class :"+c.getName());
// not a valid path problem
}} else
{ errors.add("No Path specified against method : "+methods[e].toString()+" of class : "+c.getName());
// value against path annotation is null
}}}}
catch(Throwable throwable)
{
return;
}}
static public boolean isValidPath(String path)
{
String v="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/";
if(path==null || path.length()<=3) return false;
if(path.charAt(0)!='/') return false;
if(path.charAt(path.length()-1)=='/') return false;
for(int x=0;x<path.length();x++)
{ if(v.indexOf(path.charAt(x))==-1) return false;
} if(path.indexOf("//")!=-1) return false;
return true;
}
private static boolean isDirectoryAware(Class c)
{
Class interfaces[]=c.getInterfaces();
for(Class m:interfaces)
{ if(m.getName().equals("com.thinking.machines.tmws.DirectoryAware"))
{
return true;
}}
return false;
}
private static boolean isFilesAware(Class c)
{
Class interfaces[]=c.getInterfaces();
for(Class m:interfaces)
{ if(m.getName().equals("com.thinking.machines.tmws.FilesAware"))
{
return true;
}}
return false;
}
private static boolean isSessionAware(Class c)
{
Class interfaces[]=c.getInterfaces();
for(Class m:interfaces)
{ if(m.getName().equals("com.thinking.machines.tmws.SessionAware"))
{
return true;
}}
return false;
}
private static boolean isApplicationAware(Class c)
{
Class interfaces[]=c.getInterfaces();
for(Class m:interfaces)
{ if(m.getName().equals("com.thinking.machines.tmws.ApplicationAware"))
{
return true;
}}
return false;
}
private static boolean isRequestAware(Class c)
{
Class interfaces[]=c.getInterfaces();
for(Class m:interfaces)
{ if(m.getName().equals("com.thinking.machines.tmws.RequestAware"))
{
return true;
}}
return false;
}
private static boolean isResponseAware(Class c)
{
Class interfaces[]=c.getInterfaces();
for(Class m:interfaces)
{ if(m.getName().equals("com.thinking.machines.tmws.ResponseAware"))
{
return true;
}}
return false;
}
private void createWebServiceReport(List<String> errors)
{ try
{
String basePath=getServletContext().getRealPath("/");
File reportDirectory=new File(basePath+"WEB-INF/tmws_report");
if(reportDirectory.exists()==false) reportDirectory.mkdir();
HashMap<String,PathProcessor> paths;
paths=(HashMap<String,PathProcessor>)getServletContext().getAttribute("paths");
if(paths==null)
{
paths=new HashMap<String,PathProcessor>();
}
HashMap<Class,List<PathProcessor>> classWiseMap;
classWiseMap=new HashMap<Class,List<PathProcessor>>();
List<PathProcessor> list;
Iterator<Map.Entry<String,PathProcessor>> iterator;
Map.Entry<String,PathProcessor> pair;
String path;
Class c;
PathProcessor pathProcessor;
iterator=paths.entrySet().iterator();
while(iterator.hasNext())
{
pair=iterator.next();
pathProcessor=pair.getValue();
c=pathProcessor.getMappedClass();
list=classWiseMap.get(c);
if(list==null)
{ list=new ArrayList<PathProcessor>();
classWiseMap.put(c,list);
}
list.add(pathProcessor);
}
File reportFile=new File(basePath+"/WEB-INF/tmws_report/tm_ws_report.pdf");
if(reportFile.exists()) reportFile.delete();
com.itextpdf.text.Document d=new com.itextpdf.text.Document();
com.itextpdf.text.pdf.PdfWriter.getInstance(d,new FileOutputStream(reportFile.getAbsolutePath()));
d.open();
com.itextpdf.text.Paragraph reportTitle,reportFooter;
com.itextpdf.text.pdf.PdfPTable table=null;
com.itextpdf.text.pdf.PdfPCell cell1,cell2;
Map.Entry<Class,List<PathProcessor>> classWisePair;
Iterator<Map.Entry<Class,List<PathProcessor>>> classWiseIterator;
classWiseIterator=classWiseMap.entrySet().iterator();
int pageSize=35;
int rowsGenerated=0;
boolean newPage=true;
PathProcessor pp;
float f[]={3.0f,20.0f};
int i=0;
com.itextpdf.text.Font dataTitleFont=new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,12,com.itextpdf.text.Font.BOLD);
com.itextpdf.text.Font dataFont=new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,12);
com.itextpdf.text.Font reportTitleFont=new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,16,com.itextpdf.text.Font.BOLD);
com.itextpdf.text.Font reportFooterFont=new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN,10,com.itextpdf.text.Font.BOLD);
boolean hasNext=classWiseIterator.hasNext();
while(hasNext)
{ classWisePair=classWiseIterator.next();
c=classWisePair.getKey();
list=classWisePair.getValue();
if(newPage)
{
newPage=false;
reportTitle=new com.itextpdf.text.Paragraph("Class wise list of paths configured",reportTitleFont);
reportTitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
d.add(reportTitle);
d.add(new com.itextpdf.text.Paragraph(" "));
table=new com.itextpdf.text.pdf.PdfPTable(2);
table.setWidths(f);
rowsGenerated=0;
} cell1=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph((i+1)+"."));
cell1.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
cell2=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph("Class : "+c.getName(),dataTitleFont));
table.addCell(cell1);
table.addCell(cell2);
rowsGenerated++;
for(int k=0;k<list.size();k++)
{
pp=list.get(k);
cell1=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph("Path",dataTitleFont));
cell1.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
cell2=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph(pp.getPath(),dataFont));
table.addCell(cell1);
table.addCell(cell2);
rowsGenerated++;
cell1=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph("Method",dataTitleFont));
cell1.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
cell2=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph(pp.getMethod().toString(),dataFont));
table.addCell(cell1);
table.addCell(cell2);
rowsGenerated++;
String awarenessString="";
if(pp.isApplicationAware() && pp.isSessionAware() && pp.isDirectoryAware())
{ awarenessString=" and method is ApplicationAware,SessionAware and DirectoryAware";
}else if(pp.isApplicationAware() && pp.isSessionAware())
{ awarenessString=" and method is ApplicationAware and SessionAware";
}else if(pp.isApplicationAware() && pp.isDirectoryAware())
{ awarenessString=" and method is ApplicationAware and DirectoryAware";
}else if(pp.isSessionAware() && pp.isDirectoryAware())
{ awarenessString=" and method is SessionAware and DirectoryAware";
}else if(pp.isApplicationAware())
{ awarenessString=" and method is ApplicationAware";
}else if(pp.isSessionAware())
{ awarenessString=" and method is SessionAware";
}else if(pp.isDirectoryAware())
{ awarenessString=" and method is DirectoryAware";
} cell1=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph("Produces",dataTitleFont));
cell1.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
cell2=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph(pp.produces().toString()+" "+awarenessString,dataFont));
table.addCell(cell1);
table.addCell(cell2);
rowsGenerated++;
cell1=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph(" "));
cell2=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph(" "));
table.addCell(cell1);
table.addCell(cell2);
rowsGenerated++;
if(rowsGenerated>=pageSize && k<list.size()-1)
{
d.add(table);
reportFooter=new com.itextpdf.text.Paragraph("TM Web Service Framework : Product of Thinking Machines - India",reportFooterFont);
reportFooter.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
d.add(reportFooter);
d.newPage();
reportTitle=new com.itextpdf.text.Paragraph("Class wise list of paths configured",reportTitleFont);
reportTitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
d.add(reportTitle);
d.add(new com.itextpdf.text.Paragraph(" "));
table=new com.itextpdf.text.pdf.PdfPTable(2);
table.setWidths(f);
newPage=false;
cell1=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph("contd..."));
cell1.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
cell2=new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Paragraph("Class : "+c.getName(),dataTitleFont));
table.addCell(cell1);
table.addCell(cell2);
rowsGenerated=1;
}}
hasNext=classWiseIterator.hasNext();
i++;
if(rowsGenerated>=pageSize || hasNext==false)
{
d.add(table);
reportFooter=new com.itextpdf.text.Paragraph("TM Web Service Framework : Product of Thinking Machines - India",reportFooterFont);
reportFooter.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
d.add(reportFooter);
if(hasNext)
{
d.newPage();
newPage=true;
}}} // errors portion starts
d.newPage();
newPage=true;
rowsGenerated=0;
i=0;
d.close();
}catch(Exception e)
{
System.out.println(e);
}
}
public void genrateJQueryJavaScriptFile()
{
try
{




ServletContext servletContext=getServletContext();
String jsFileFolderName=getInitParameter("jsFile-folder");
File jsFileFolder=new File(servletContext.getRealPath("/")+"WEB-INF"+File.separator+jsFileFolderName);
File file=null;
if(!jsFileFolder.exists())
{
file=new File(servletContext.getRealPath("/")+"WEB-INF"+File.separator+"tmJquery.js");
}
else 
{
System.out.println(jsFileFolder.getAbsolutePath());
file=new File(jsFileFolder.getAbsolutePath()+File.separator+"tmJquery.js");
}
HashMap<Class,List<PathProcessor>> classWiseMap;
classWiseMap=new HashMap<Class,List<PathProcessor>>();
List<PathProcessor> list;
Iterator<Map.Entry<String,PathProcessor>> iterator;
Map.Entry<String,PathProcessor> pair;
String path;
Class c;
PathProcessor pathProcessor;
HashMap<String,PathProcessor> paths=(HashMap<String,PathProcessor>)servletContext.getAttribute("paths");
iterator=paths.entrySet().iterator();
while(iterator.hasNext())
{
pair=iterator.next();
pathProcessor=pair.getValue();
c=pathProcessor.getMappedClass();
list=classWiseMap.get(c);
if(list==null)
{
list=new ArrayList<PathProcessor>();
classWiseMap.put(c,list);
}
list.add(pathProcessor);
}
if(file.exists()) file.delete();
RandomAccessFile raf=new RandomAccessFile(file,"rw");
Iterator<Map.Entry<Class,List<PathProcessor>>> classWiseMapIterator;
classWiseMapIterator=classWiseMap.entrySet().iterator();
Map.Entry<Class,List<PathProcessor>> mapPair;
ArrayList<PathProcessor> pathProcessors;
Iterator<PathProcessor> pathProcessorsIterator=null;
while(classWiseMapIterator.hasNext())
{
mapPair=classWiseMapIterator.next();
String functionName=((Path)mapPair.getKey().getAnnotation(Path.class)).value();
raf.writeBytes("function "+functionName.substring(functionName.lastIndexOf("/")+1,functionName.length())+"() \r\n");
raf.writeBytes("{");
pathProcessors=(ArrayList<PathProcessor>)mapPair.getValue();
pathProcessorsIterator=pathProcessors.iterator();
int parameterCount=0;
int x=0;
while(pathProcessorsIterator.hasNext())
{
pathProcessor=pathProcessorsIterator.next();
if(!pathProcessor.isFilesAware())
{
raf.writeBytes("this."+((Path)pathProcessor.getMethod().getAnnotation(Path.class)).value()+"=function(");
parameterCount=pathProcessor.getParameterTypes().length;
x=0;
while(x<parameterCount)
{
raf.writeBytes("parameter"+(x+1));
x++;
if(x<parameterCount)
{
raf.writeBytes(",");
}
}
if(x>0)
{
raf.writeBytes(",");
}
raf.writeBytes("func,exc,err)\r\n");
raf.writeBytes("{\r\n");
raf.writeBytes("$.ajax({\r\n");
raf.writeBytes("'type' : ");
if(pathProcessor.allowPostTypeRequest() && pathProcessor.allowGetTypeRequest())
{
raf.writeBytes("'POST',\r\n");
}
else if(pathProcessor.allowGetTypeRequest())
{
raf.writeBytes("'GET',\r\n");
}
else
{
raf.writeBytes("'POST',\r\n");
}
raf.writeBytes("'url' : ");
raf.writeBytes("'tmws"+pathProcessor.getPath()+"',\r\n");
if(x>0)
{
raf.writeBytes("'data' : {\r\n");
x=0;
while(x<parameterCount)
{
raf.writeBytes("'argument-"+(x+1)+"' : parameter"+(x+1));
x++;
if(x<parameterCount)
{
raf.writeBytes(",");
}
}
raf.writeBytes("},\r\n");
}
raf.writeBytes("'success' : function(data){\r\n");
raf.writeBytes("if(data.success){\r\n");
raf.writeBytes("if(data.isReturningSomething) func(data.result);\r\n");
raf.writeBytes("else if(data.isException) exc(data.exception);}\r\n");
raf.writeBytes("else{\r\n");
raf.writeBytes("err(data.error);}\r\n");
raf.writeBytes("},\r\n");
raf.writeBytes("error : function(){ error(\"cannot send request\");}");
raf.writeBytes("});\r\n");
}
raf.writeBytes("}\r\n");
}
raf.writeBytes("}\r\n");
}
raf.close();
}catch(Exception exception)
{
System.out.println(exception);
}
}
}