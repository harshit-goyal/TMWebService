package com.thinking.machines.tmws;
import javax.servlet.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import javax.servlet.http.*;
import org.apache.catalina.core.*;
class RequestProcessor
{
private RequestProcessor()
{
}
static void process(ServletContext servletContext,HttpServletRequest request,HttpServletResponse response,HashMap<String,PathProcessor> paths)
{
try
{
String uri=request.getRequestURI();
String servletPath=request.getServletPath();
String contextPath=request.getServletContext().getContextPath();
String requestPath=uri.substring((contextPath+servletPath).length());
if(requestPath.equals("/webservice-report"))
{
Boolean developmentMode=(Boolean)servletContext.getAttribute("developmentMode");
if(developmentMode==false)
{
sendErrorResponse(HttpServletResponse.SC_NOT_FOUND,request,response);
return;
}
processWebServiceReportRequest(request,response,servletContext.getRealPath("/"));
return;
}
PathProcessor pathProcessor=paths.get(requestPath);
if(pathProcessor==null)
{
sendErrorResponse(HttpServletResponse.SC_NOT_FOUND,request,response);
return;
}
String typeOfRequest=request.getMethod();
if(typeOfRequest.equals("POST"))
{
if(pathProcessor.allowPostTypeRequest()==false)
{
sendErrorResponse(HttpServletResponse.SC_METHOD_NOT_ALLOWED,request,response);
return;
}
}
else if(typeOfRequest.equals("GET"))
{
if(pathProcessor.allowGetTypeRequest()==false)
{
sendErrorResponse(HttpServletResponse.SC_METHOD_NOT_ALLOWED,request,response);
return;
}
}
else
{
sendErrorResponse(HttpServletResponse.SC_METHOD_NOT_ALLOWED,request,response);
return;
}
ObjectNode errorResponseObjectNode;
ObjectNode responseObjectNode;
ObjectMapper objectMapper=new ObjectMapper();
objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
Class [] parameterTypes=pathProcessor.getParameterTypes();
Object object=pathProcessor.getObject();
if(object==null)
{ // unable to instantiate the object of the class mapped to the PATH
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("error","Unable to create instance of : "+pathProcessor.getMappedClass().getName());
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
// dependency injection
if(pathProcessor.isDirectoryAware())
{
Class c=object.getClass();
Method m=c.getMethod("setDirectory",File.class);
m.invoke(object,new File(servletContext.getRealPath("/")));

}
if(pathProcessor.isSessionAware())
{
Class c=object.getClass();
Method m=c.getMethod("setHttpSession",HttpSession.class);
m.invoke(object,request.getSession());
}
if(pathProcessor.isApplicationAware())
{
Class c=object.getClass();
Method m=c.getMethod("setServletContext",ServletContext.class);
m.invoke(object,servletContext);
}
if(pathProcessor.isRequestAware())
{
Class c=object.getClass();
Method m=c.getMethod("setHttpServletRequest",HttpServletRequest.class);
m.invoke(object,request);
}
if(pathProcessor.isResponseAware())
{
Class c=object.getClass();
Method m=c.getMethod("setHttpServletResponse",HttpServletResponse.class);
m.invoke(object,response);
}
ArrayList<FileUploadWrapper> files=null;
if(pathProcessor.isFilesAware())
{
files=new ArrayList<FileUploadWrapper>();
Collection<javax.servlet.http.Part> collection=request.getParts();
Iterator i=collection.iterator();
ApplicationPart p=null;
FileUploadWrapper file;
InputStream inputStream;
FileOutputStream fileOutputStream;
byte[] b=new byte[1024];
while(i.hasNext())
{
System.out.println("Application Part nikla --------");
p=(ApplicationPart)i.next();
if(p.getFilename()!=null)
{
String newFileName=UUID.randomUUID().toString().replaceAll("-","k");
long contentLength=p.getSize();
int byteCount=0;
int bytesRead=0;
File f=new File(servletContext.getRealPath("/")+"/WEB-INF/"+newFileName+(p.getFilename().substring(p.getFilename().lastIndexOf("."),p.getFilename().length())));
fileOutputStream=new FileOutputStream(f);
inputStream=p.getInputStream();
while(true)
{
byteCount=inputStream.read(b);
if(byteCount<0) break;
bytesRead+=byteCount;
fileOutputStream.write(b,0,byteCount);
if(bytesRead==contentLength) break;
}
inputStream.close();
fileOutputStream.close();
file=new FileUploadWrapper(f,p.getFilename());
files.add(file);
System.out.println("one added as : "+p.getFilename());
}
}
Class c=object.getClass();
Method m=null;
try
{
m=c.getMethod("setFiles",FileUploadWrapper[].class);
}catch(Exception e)
{
}
try
{
FileUploadWrapper filesWrapper[]=new FileUploadWrapper[files.size()];
filesWrapper=files.toArray(filesWrapper);
m.invoke(object,(Object)filesWrapper);
}catch(Exception e)
{
}
}
ObjectNode objectNode=null;
objectNode=getRequestParameters(request);
if(objectNode==null)
{
System.out.println("object node null h  ");
StringBuffer sb=new StringBuffer();
BufferedReader br=request.getReader();
String line=null;
while(true)
{
line=br.readLine();
if(line==null) break;
sb.append(line);
}
String rawData=sb.toString();
System.out.println("Raw data : ("+rawData+")");
if(rawData==null || rawData.length()==0)
{
rawData="{}";
}
try
{
objectNode=objectMapper.readValue(rawData,ObjectNode.class);
}catch(Exception exception)
{
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("error","Invalid JSON in request");
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
// if ends to extract raw data because parameters doesn't exist in QS
Iterator<String> iterator=objectNode.fieldNames();
ArrayList<String> parameters=new ArrayList<String>();
while(iterator.hasNext()) parameters.add(iterator.next());
Method method=pathProcessor.getMethod();
Object [] arguments;
Object result;
if(parameterTypes.length==parameters.size())
{
arguments=new Object[parameterTypes.length];
JsonNode argument;
for(int i=0;i<parameterTypes.length;i++)
{
argument=objectNode.get("argument-"+(i+1));
if(argument==null)
{
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("error","argument-"+(i+1)+" is missing");
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
else
{
try
{
arguments[i]=objectMapper.readValue(argument.toString(),parameterTypes[i]);
}
catch(Throwable ee)
{
ee.printStackTrace();
}
}
}
try
{
result=method.invoke(object,arguments);
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
if(pathProcessor.isReturningSomething())
{
if(result!=null && result instanceof Exception)
{
Method getExceptionsMethod=null;
try
{
getExceptionsMethod=result.getClass().getMethod("getExceptions");
result=getExceptionsMethod.invoke(result);
}catch(Exception exception)
{
}
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",true);
errorResponseObjectNode.put("isObject",true);
String jsonString=objectMapper.writeValueAsString(result);
JsonNode jsonNode=objectMapper.readTree(jsonString);
errorResponseObjectNode.putPOJO("exception",jsonNode);
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
else
{
if(result instanceof ActionForword)
{
RequestDispatcher requestDispatcher;
requestDispatcher=request.getRequestDispatcher(((ActionForword)result).getResourceName());
requestDispatcher.forward(request,response);
}
if(result instanceof ResponseWrapper)
{
return;
}
if(result instanceof FileDownloadWrapper)
{
FileDownloadWrapper fileDownloadWrapper=(FileDownloadWrapper)result;
File file=fileDownloadWrapper.getFile();
try
{
response.setContentType(fileDownloadWrapper.getContentType());
if(fileDownloadWrapper.isAttachment())
{
response.setHeader("Content-Disposition","attachment;filename="+fileDownloadWrapper.getFileName());
} else
{
response.setHeader("Content-Disposition","filename="+fileDownloadWrapper.getFileName());
}
response.setContentLength((int)file.length());
FileInputStream fileInputStream = new FileInputStream(file);
OutputStream outputStream = response.getOutputStream();
byte[] buffer = new byte[1024];
int count = 0;
while (true)
{ count=fileInputStream.read(buffer);
if(count<=0)
{
break;
}
outputStream.write(buffer, 0, count);
}
fileInputStream.close();
outputStream.close();
}catch(IOException ioException)
{
System.out.println(ioException); // remove after testing
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
return;
} catch(Exception exception)
{
System.out.println(exception); // remove after testing
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
return;
}
}
else
{
responseObjectNode=objectMapper.createObjectNode();
responseObjectNode.put("success",true);
responseObjectNode.put("isReturningSomething",true);
if(result!=null)
{
responseObjectNode.put("isResultNull",false);
String jsonString=objectMapper.writeValueAsString(result);
JsonNode jsonNode=objectMapper.readTree(jsonString);
responseObjectNode.putPOJO("result",jsonNode);
}
else
{
responseObjectNode.put("isResultNull",true);
}

PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(responseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
}
else
{
responseObjectNode=objectMapper.createObjectNode();
responseObjectNode.put("success",true);
responseObjectNode.put("isReturningSomething",false);
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(responseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
catch(IllegalAccessException illegalAccessException)
{
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("isObject",false);
errorResponseObjectNode.put("error","Cannot access : "+method.toString());
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
catch(InvocationTargetException invocationTargetException)
{
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",true);
errorResponseObjectNode.put("isObject",false);
errorResponseObjectNode.put("exception",invocationTargetException.getCause().toString());
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();

return;
}}
else if(parameterTypes.length==1)
{ arguments=new Object[1];
// replace rawData with objectNode converted to String as JSON
//arguments[0]=objectMapper.readValue(rawData,parameterTypes[0]);
System.out.println("cool fool tool : "+objectNode.toString());
arguments[0]=objectMapper.readValue(objectNode.toString(),parameterTypes[0]);
try
{
result=method.invoke(object,arguments);
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
if(pathProcessor.isReturningSomething())
{
if(result!=null && result instanceof Exception)
{
Method getExceptionsMethod=null;
try
{
getExceptionsMethod=result.getClass().getMethod("getExceptions");
result=getExceptionsMethod.invoke(result);
}catch(Exception exception)
{
}
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",true);
errorResponseObjectNode.put("isObject",true);
String jsonString=objectMapper.writeValueAsString(result);
JsonNode jsonNode=objectMapper.readTree(jsonString);
errorResponseObjectNode.putPOJO("exception",jsonNode);
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}else
{
if(result instanceof ActionForword)
{
RequestDispatcher requestDispatcher;
requestDispatcher=request.getRequestDispatcher(((ActionForword)result).getResourceName());
requestDispatcher.forward(request,response);
}
if(result instanceof ResponseWrapper)
{
return;
}
if(result instanceof FileDownloadWrapper)
{
FileDownloadWrapper fileDownloadWrapper=(FileDownloadWrapper)result;
File file=fileDownloadWrapper.getFile();
try
{
response.setContentType(fileDownloadWrapper.getContentType());
if(fileDownloadWrapper.isAttachment())
{
response.setHeader("Content-Disposition","attachment;filename="+fileDownloadWrapper.getFileName());
} else
{
response.setHeader("Content-Disposition","filename="+fileDownloadWrapper.getFileName());
}
response.setContentLength((int)file.length());
FileInputStream fileInputStream = new FileInputStream(file);
OutputStream outputStream = response.getOutputStream();
byte[] buffer = new byte[1024];
int count = 0;
while (true)
{ count=fileInputStream.read(buffer);
if(count<=0)
{
break;
}
outputStream.write(buffer, 0, count);
}
fileInputStream.close();
outputStream.close();
}catch(IOException ioException)
{
System.out.println(ioException); // remove after testing
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
return;
} catch(Exception exception)
{
System.out.println(exception); // remove after testing
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
return;
}
}
else
{
responseObjectNode=objectMapper.createObjectNode();
responseObjectNode.put("success",true);
responseObjectNode.put("isReturningSomething",true);
if(result!=null)
{
responseObjectNode.put("isResultNull",false);
System.out.println("result m : "+objectMapper.writeValueAsString(result));
responseObjectNode.put("result",objectMapper.writeValueAsString(result));
}
else
{
responseObjectNode.put("isResultNull",true);
}
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(responseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
}else
{
responseObjectNode=objectMapper.createObjectNode();
responseObjectNode.put("success",true);
responseObjectNode.put("isReturningSomething",false);
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(responseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
catch(IllegalAccessException illegalAccessException)
{
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("isObject",false);
errorResponseObjectNode.put("error","Cannot access : "+method.toString());
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
catch(InvocationTargetException invocationTargetException)
{
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",true);
errorResponseObjectNode.put("isObject",false);
errorResponseObjectNode.put("exception",invocationTargetException.getCause().toString());
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
else
{
if(pathProcessor.isFilesAware() && files.size()>0) removeTemporeryFile(files.<FileUploadWrapper>toArray(new FileUploadWrapper[1]));
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("isObject",false);
errorResponseObjectNode.put("error","Expected : "+parameterTypes.length+" arguments, found : "+parameters.size());
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
return;
}
}
catch(Exception exception)
{
System.out.println(exception);
}
}
static private void processWebServiceReportRequest(HttpServletRequest request,HttpServletResponse response,String basePath)
{
try
{
File file=new File(basePath+"WEB-INF/tmws_report/tm_ws_report.pdf");
if(file.exists()==false)
{
sendErrorResponse(HttpServletResponse.SC_NOT_FOUND,request,response);
return;
}
response.setContentType("application/pdf");
FileInputStream fileInputStream=new FileInputStream(file);
OutputStream outputStream=response.getOutputStream();
byte buffer[]=new byte[1024];
int count;
while(true)
{
count=fileInputStream.read(buffer);
if(count<=0) break;
outputStream.write(buffer,0,count);
}
fileInputStream.close();
outputStream.close();
}catch(Exception e)
{
System.out.println(e);
}
}
static private void sendErrorResponse(int responseFlag,HttpServletRequest
request,HttpServletResponse response)
{
try
{
ObjectNode errorResponseObjectNode;
ObjectMapper objectMapper=new ObjectMapper();
if(responseFlag==HttpServletResponse.SC_NOT_FOUND)
{
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("error","Resource not found");
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
}
if(responseFlag==HttpServletResponse.SC_METHOD_NOT_ALLOWED)
{
errorResponseObjectNode=objectMapper.createObjectNode();
errorResponseObjectNode.put("success",false);
errorResponseObjectNode.put("isException",false);
errorResponseObjectNode.put("error",request.getMethod()+" type request not allowed.");
PrintWriter printWriter=response.getWriter();
response.setContentType("application/json");
printWriter.print(errorResponseObjectNode.toString());
printWriter.flush();
printWriter.close();
}
}
catch(Exception excetion)
{
}
}
static public ObjectNode getRequestParameters(HttpServletRequest request)
{
Enumeration<String> enumerator=request.getParameterNames();
String parameterName;
LinkedList<String> names=new LinkedList<String>();
while(enumerator.hasMoreElements())
{
parameterName=enumerator.nextElement();
System.out.println(parameterName);
names.add(parameterName);

}
if(names.size()==0) return null;
ObjectMapper objectMapper=new ObjectMapper();
ObjectNode objectNode=objectMapper.createObjectNode();
String data[];
for(String name:names)
{
data=request.getParameterValues(name);
if(data.length==0) continue;
if(data.length==1)
{
objectNode.put(name,data[0]);
} else
{
 // assignment
}
}
return objectNode;
}

public static void removeTemporeryFile(FileUploadWrapper[] fileUploadWrappers)
{
for(FileUploadWrapper fileUploadWrapper  : fileUploadWrappers)
{
System.out.println("deepesh phle");
if(fileUploadWrapper.isTemporery()) fileUploadWrapper.getFile().delete();
System.out.println("deepesh");
}
System.out.println("deepesh bhar vala");
}
}