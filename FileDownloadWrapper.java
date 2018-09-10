package com.thinking.machines.tmws;
import java.io.*;
public class FileDownloadWrapper
{
private File file;
private String fileName;
private String contentType;
private boolean isAttachment;
public FileDownloadWrapper()
{
this.file=null;
this.fileName=null;
}
public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}
public void setFileName(String fileName)
{
this.fileName=fileName;
}
public String getFileName()
{
return this.fileName;
}
public void setContentType(String contentType)
{
this.contentType=contentType;
}
public String getContentType()
{
return this.contentType;
}
public void isAttachment(boolean isAttachment)
{
this.isAttachment=isAttachment;
}
public boolean isAttachment()
{
return this.isAttachment;
}
}