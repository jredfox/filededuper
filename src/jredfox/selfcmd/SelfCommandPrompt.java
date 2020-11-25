package jredfox.selfcmd;

import java.io.Console;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import jredfox.filededuper.Main;
import jredfox.filededuper.util.IOUtils;
/**
 * @author jredfox. Credits to Chocohead#7137 for helping
 * this class is a wrapper for your program. It fires command prompt and stops it from quitting without user input
 */
public class SelfCommandPrompt {
	
	public static final String VERSION = "1.2.2";
	
	/**
	 * args are [shouldPause, mainClass, programArgs]
	 */
	public static void main(String[] args)
	{
		boolean shouldPause = Boolean.parseBoolean(args[0]);
		
		try
		{
			Class<?> mainClass = Class.forName(args[1]);
			String[] programArgs = new String[args.length - 2];
			System.arraycopy(args, 2, programArgs, 0, programArgs.length);
			Method method = mainClass.getMethod("main", String[].class);
			method.invoke(null, new Object[]{programArgs});
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		
		if(shouldPause)
		{
			Scanner old = new Scanner(System.in);
			Scanner scanner = old.useDelimiter("\n");
			System.out.println("Press ENTER to continue:");
			scanner.next();
			old.close();
			scanner.close();
		}
	}

	/**
	 * supports all platforms no need to reboot, supports debugging and all ides,
	 * and supports shutdown hooks
	 */
	public static void runWithJavaCMD(String appTitle, boolean onlyCompiled)
	{
		//TODO:
	}
	
	/**
	 * reboot your application with a command prompt terminal. Note if you hard code your mainClass instead of using the above method it won't support all compilers like eclipse's jar in jar loader
	 * NOTE: doesn't support debug function as it breaks ides connection proxies to the jvm agent's debug.
	 * before calling this if you have jvmArguments for like ports or connections close them before rebooting
	 */
	public static void runwithCMD(String[] args, String appName, String appId, boolean onlyCompiled, boolean pause)
	{
		runwithCMD(getMainClass(), args, appName, appId, onlyCompiled, pause);
	}
	
	/**
	 * reboot your application with a command prompt terminal. Note if you hard code your mainClass instead of using the above method it won't support all compilers like eclipse's jar in jar loader
	 * NOTE: doesn't support debug function as it breaks ides connection proxies to the jvm agent's debug.
	 * before calling this if you have jvmArguments for like ports or connections close them before rebooting
	 */
	public static void runwithCMD(Class<?> mainClass, String[] args, String appName, String appId, boolean onlyCompiled, boolean pause) 
	{
		if(isDebugMode() || getMainClassName().equals(SelfCommandPrompt.class.getName()))
			return;
        Console console = System.console();
        if(console == null)
        {
            try
            {	
            	boolean compiled = isCompiled(mainClass);
            	if(!compiled && onlyCompiled)
            		return;
            	
            	String str = getProgramArgs(args, " ");
            	String argsStr = " " + mainClass.getName() + (str.isEmpty() ? "" : " " + str);
            	String jvmArgs = getJVMArgs();
            	String os = System.getProperty("os.name").toLowerCase();
            	String command = "java " + (jvmArgs.isEmpty() ? "" : jvmArgs + " ") + "-cp " + System.getProperty("java.class.path") + " " + SelfCommandPrompt.class.getName() + " " + pause + argsStr;
            	
            	if(os.contains("windows"))
            	{
            		File bat = new File(System.getenv("APPDATA") + "/SelfCommandPrompt", appId + "-run.bat");
            		bat.getParentFile().mkdirs();
            		List<String> list = new ArrayList(1);
            		list.add("@echo off");
            		list.add("start" + " \"" + appName + "\" " + command);
            		IOUtils.saveFileLines(list, bat, true);
            		ProcessBuilder pb = new ProcessBuilder(bat.getAbsolutePath());
            		//inherit IO and main directory
            		pb.directory(getProgramDir());
            		//fire the batch file
            		pb.start();
            		System.exit(0);
            	}
            	else
            	{
            		SelfCommandPrompt.runWithJavaCMD(appName, onlyCompiled);//other os's seem to break or don't have the start command
            	}
			}
            catch (Exception e)
            {
				e.printStackTrace();
			}
        }
	}
	
	/**
	 * checks if the jar is compiled based on the main class
	 * @throws UnsupportedEncodingException 
	 */
	public static boolean isCompiled() throws UnsupportedEncodingException
	{
		return isCompiled(getMainClass());
	}
	
	/**
	 * checks per class if the jar is compiled
	 * @throws UnsupportedEncodingException 
	 */
	public static boolean isCompiled(Class<?> mainClass) throws UnsupportedEncodingException
	{
		File file = getFileFromClass(mainClass);
		return getExtension(file).equals("jar") || getMainClassName().endsWith("jarinjarloader.JarRsrcLoader");
	}
	
	/**
	 * get a file from a class
	 */
	public static File getFileFromClass(Class<?> clazz) throws UnsupportedEncodingException
	{
		String jarPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();//get the path of the currently running jar
		String fileName = URLDecoder.decode(jarPath, "UTF-8").substring(1);
		return new File(fileName);
	}
	
	public static String getJVMArgs()
	{
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();
		StringBuilder b = new StringBuilder();
		String sep = " ";
		int index = 0;
		for(String s : arguments)
		{
			s = index + 1 != arguments.size() ? s + sep : s;
			b.append(s);
			index++;
		}
		return b.toString();
	}
	
	public static boolean isDebugMode()
	{
		return java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
	}
	
	public static String getMainClassName()
	{
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		String actualMain = main.getClassName();
		return actualMain;
	}
	
	public static Class<?> getMainClass()
	{
		Class<?> mainClass = null;
		try 
		{
			String className = getMainClassName();
			mainClass = Class.forName(className);
		} 
		catch (ClassNotFoundException e1) 
		{
			e1.printStackTrace();
		}
		return mainClass;
	}
	
	public static String getProgramArgs(String[] args, String sep) 
	{
		if(args == null)
			return null;
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(String s : args)
		{
			String q = s.contains(" ") ? "\"" : "";
			s = index + 1 != args.length ? (q + s + q + sep) : (q + s + q);
			b.append(s);
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
		return new File(System.getProperty("user.dir")).getAbsoluteFile();
	}
}