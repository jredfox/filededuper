package jredfox.selfcmd;

import java.io.Console;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import jredfox.filededuper.util.DeDuperUtil;

public class SelfCommandPrompt {
	/**
	 * run your current program with command prompt and close your current program without one
	 */
	public static void runwithCMD(Class mainClass, String[] args, String appTitle, boolean onlyCompiled) 
	{
        Console console = System.console();
        if(console == null)
        {
            try 
            {
            	String argsStr = toString(args, " ");
            	String jarPath = mainClass.getProtectionDomain().getCodeSource().getLocation().getPath();// get the path of the currently running jar
            	String filename = URLDecoder.decode(jarPath, "UTF-8").substring(1);
            	File file = new File(filename);
            	boolean compiled = getExtension(file).equals("jar");
            	if(!compiled && onlyCompiled)
            	{
            		return;
            	}
            	String os = System.getProperty("os.name").toLowerCase();
            	if(os.contains("windows"))
            	{
            		new ProcessBuilder("cmd", "/c", "start", "\"" + appTitle + "\"", "cmd", "/c", ("java " + "-jar " + filename + (argsStr.isEmpty() ? "" : " " + argsStr) + " & pause")).start();
            	}
            	else if(os.contains("mac"))
            	{
            		new ProcessBuilder(new String[] {"/bin/bash", "-c", "java", "-jar", filename}).start();
            	}
            	else if(os.contains("linux"))
            	{
            		new ProcessBuilder(new String[] {"xfce4-terminal", "--title=" + appTitle, "--hold", "-x", "java", "-jar", filename}).start();
            	}
			}
            catch (Exception e)
            {
				e.printStackTrace();
			}
            System.exit(0);
        }
	}
	
	public static String toString(String[] args, String sep) 
	{
		if(args == null)
			return null;
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(String s : args)
		{
			b.append(index + 1 != args.length ? s + sep : s);
			index++;
		}
		return b.toString();
	}
	
	/**
	 * get a file extension. Note directories do not have file extensions
	 */
	public static String getExtension(File file) 
	{
		String name = file.getName();
		int index = name.lastIndexOf('.');
		return index != -1 && !file.isDirectory() ? name.substring(index + 1) : "";
	}
	
	public static File getProgramDir()
	{
		return new File(System.getProperty("user.dir"));
	}
}
