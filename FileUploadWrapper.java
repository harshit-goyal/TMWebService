package com.thinking.machines.tmws;
import java.io.*;
public class FileUploadWrapper
{
private File file;
private String realFileName;
private boolean isTemporery;
private boolean isOverflowed;
public FileUploadWrapper(File file,String realFileName)
{
this.file=file;
this.realFileName=realFileName;
}
public void isOverflowed(boolean isOverflowed)
{
this.isOverflowed=isOverflowed;
}
public boolean isOverflowed()
{
return this.isOverflowed;
}
public void isTemporery(boolean isTemporery)
{
this.isTemporery=isTemporery;
}
public boolean isTemporery()
{
return this.isTemporery;
}
public String getFileName()
{
return this.file.getName();
}
public String getRealFileName()
{
return this.realFileName;
}
public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}
public long fileSize()
{
return this.file.length();
}
}