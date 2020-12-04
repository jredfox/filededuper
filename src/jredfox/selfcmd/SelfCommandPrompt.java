package jredfox.selfcmd;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.selfcmd.exe.ExeBuilder;
import jredfox.selfcmd.jconsole.JConsole;
import jredfox.selfcmd.util.OSUtil;
/**
 * @author jredfox. Credits to Chocohead#7137 for helping me find the windows start command & System.getProperty("java.class.path);
 * this class is a wrapper for your program. It fires command prompt and stops it from quitting without user input
 */
public class SelfCommandPrompt {
	
	public static final String VERSION = "2.0.0";
	public static final String INVALID = "\"'`,";
	public static final File selfcmd = new File(OSUtil.getAppData(), "SelfCommandPrompt");
	public static String wrappedAppId;
	public static String wrappedAppName;
	public static Class<?> wrappedAppClass;
	public static String[] wrappedAppArgs;
	public static boolean wrappedPause;
	public static JConsole jconsole;
	
	static
	{
		syncConfig();
	}
	
	/**
	 * args are [shouldPause, mainClass, programArgs...]
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args)
	{
		boolean shouldPause = Boolean.parseBoolean(args[0]);
		
		try
		{
			Class<?> mainClass = Class.forName(args[1]);
			wrappedAppClass = mainClass;
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
			Scanner scanner = new Scanner(System.in).useDelimiter("\n");//Warning says scanner is never closed but, useDelimiter returns itself
			System.out.println("Press ENTER to continue:");
			if(scanner.hasNext())
				scanner.next();
			scanner.close();
		}
	}

	/**
	 * NOTE: this is WIP and doesn't support System.in redirect yet, and there are many other issues with it
	 */
	public static JConsole startJConsole(String appId, String appName)
	{	
		if(jconsole != null)
			throw new RuntimeException("jconsole has already started!");
		JConsole console = new JConsole(appName)
		{
			@Override
			public boolean isJavaCommand(String[] command){return true;}//always return true we do not support os commands in JConsole

			@Override
			public boolean shutdown(){return true;}
		};
		console.setEnabled(true);
		jconsole = console;
		System.out.println("JCONSOLE isn't working yet. Please check back in a future version ;)");
		return jconsole;
	}
	
	/**
	 * ensure your program boots up with a command prompt terminal either a native configurable os terminal or JConsole.
	 * If you hard code your main class it won't support wrappers like eclipe's jar in jar loader.
	 * if you have connections in jvm args close them before reboot if {@link SelfCommandPrompt#hasJConsole()} returns false
	 * @Since 2.0.0
	 */
	public static void runWithCMD(String appId, String appName, String[] args)
	{
		runWithCMD(appId, appName, args, false, true);
	}
	
	/**
	 * ensure your program boots up with a command prompt terminal either a native configurable os terminal or JConsole.
	 * If you hard code your main class it won't support wrappers like eclipe's jar in jar loader.
	 * if you have connections in jvm args close them before reboot !SelfCommandPrompt#hasJConsole()
	 * @Since 2.0.0
	 */
	public static void runWithCMD(String appId, String appName, String[] args, boolean onlyCompiled, boolean pause)
	{
		runWithCMD(appId, appName, getMainClass(), args, onlyCompiled, pause);
	}
	
	/**
	 * ensure your program boots up with a command prompt terminal either a native configurable os terminal or JConsole.
	 * If you hard code your main class it won't support wrappers like eclipe's jar in jar loader.
	 * if you have connections in jvm args close them before reboot !SelfCommandPrompt#hasJConsole()
	 * @Since 2.0.0
	 */
	public static void runWithCMD(String appId, String appName, Class<?> mainClass, String[] args, boolean onlyCompiled, boolean pause) 
	{
		cacheApp(appId, appName, mainClass, args, pause);
		boolean compiled = isCompiled(mainClass);
		if(!compiled && onlyCompiled || compiled && System.console() != null || isDebugMode() || isWrapped() || jconsole != null)
		{
			return;
		}
		
        if(hasJConsole())
        {
        	startJConsole(appId, appName);
        	return;
        }
        
		try
		{
			rebootWithTerminal(appId, appName, mainClass, args, pause);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
			startJConsole(appId, appName);
		}
	}

	/**
	 * reboot the program. config sync enabled
	 */
	public static void reboot() throws IOException
	{
		reboot(wrappedAppId, wrappedAppName, wrappedAppClass, wrappedAppArgs, wrappedPause);
	}
	
	/**
	 * reboot the program with new args. config sync enabled
	 */
	public static void reboot(String[] newArgs) throws IOException
	{
		reboot(wrappedAppId, wrappedAppName, wrappedAppClass, newArgs, wrappedPause);
	}
	
	/**
	 * reboot the program. config sync enabled
	 */
	public static void reboot(String appId, String appName, Class<?> mainClass, String[] args, boolean pause) throws IOException
	{
		syncConfig();
		ExeBuilder builder = new ExeBuilder();
		if(hasJConsole())
		{
			builder.addCommand("java");
			builder.addCommand(getJVMArgs());
			builder.addCommand("-cp");
			builder.addCommand("\"" + System.getProperty("java.class.path") + "\"");//doesn't need to check parsing chars cause this is a generic reboot and if it reboots with terminal it will catch the error on boot
			builder.addCommand(mainClass.getName());
			builder.addCommand(programArgs(args));
			String command = builder.toString();
			runInTerminal(command);
			shutdown();
		}
		else
		{
			rebootWithTerminal(appId, appName, mainClass, args, pause);
		}
	}

	/**
	 * do not call this directly without checks or it will just keep rebooting and only in the terminal. Doesn't call {@link SelfCommandPrompt#cacheApp(String, String, Class, String[], boolean)}
	 */
	public static void rebootWithTerminal(String appId, String appName, Class<?> mainClass, String[] args, boolean pause) throws IOException
	{
    	if(containsAny(appId, INVALID))
    		throw new RuntimeException("appId contains illegal parsing characters:(" + appId + "), invalid:" + INVALID);
    	
            String libs = System.getProperty("java.class.path");
            if(containsAny(libs, INVALID))
            	throw new RuntimeException("one or more LIBRARIES contains illegal parsing characters:(" + libs + "), invalid:" + INVALID);
            
            ExeBuilder builder = new ExeBuilder();
        	builder.addCommand("java");
        	builder.addCommand(getJVMArgs());
        	builder.addCommand("-cp");
        	builder.addCommand("\"" + libs + "\"");
        	builder.addCommand(SelfCommandPrompt.class.getName());
        	builder.addCommand(String.valueOf(pause));
        	builder.addCommand(mainClass.getName());
        	builder.addCommand(programArgs(args));
        	String command = builder.toString();
        	runInNewTerminal(appId, appName, appId, command);
        	shutdown();
	}
	
	/**
	 * enforces it to run in the command prompt terminal as sometimes it doesn't work without it
	 */
	public static void runInTerminal(String command) throws IOException
	{
		Runtime.getRuntime().exec(terminal + " " + OSUtil.getExeAndClose() + " " + command);
	}
	
	/**
	 * runs a command in a new terminal window.
	 * the sh name is the file name you want the shell script stored. The appId is to locate your folder
	 * @Since 2.0.0
	 */
	public static void runInNewTerminal(String appId, String appName, String shName, String command) throws IOException
	{
        if(OSUtil.isWindows())
        {
        	Runtime.getRuntime().exec(terminal + " " + OSUtil.getExeAndClose() + " start " + "\"" + appName + "\" " + command);
        }
        else if(OSUtil.isMac())
        {
        	File sh = new File(getAppdata(appId), shName + ".sh");
        	List<String> cmds = new ArrayList<>();
        	cmds.add("#!/bin/bash");
        	cmds.add("set +v");//@Echo off
        	cmds.add("echo -n -e \"\\033]0;" + appName + "\\007\"");//Title
        	cmds.add("cd " + getProgramDir().getAbsolutePath());//set the proper directory
        	cmds.add(command);//actual command
        	IOUtils.saveFileLines(cmds, sh, true);//save the file
        	IOUtils.makeExe(sh);//make it executable
        	Runtime.getRuntime().exec(terminal + " " + OSUtil.getExeAndClose() + " osascript -e \"tell application \\\"Terminal\\\" to do script \\\"" + sh.getAbsolutePath() + "\\\"\"");
        }
        else if(OSUtil.isLinux())
        {
        	File sh = new File(getAppdata(appId), shName + ".sh");
        	List<String> cmds = new ArrayList<>();
        	cmds.add("#!/bin/bash");
        	cmds.add("set +v");//@Echo off
        	cmds.add("echo -n -e \"\\033]0;" + appName + "\\007\"");//Title
        	cmds.add("cd " + getProgramDir().getAbsolutePath());//set the proper directory
        	cmds.add(command);//actual command
        	IOUtils.saveFileLines(cmds, sh, true);//save the file
        	IOUtils.makeExe(sh);//make it executable
        	Runtime.getRuntime().exec(terminal + " " + OSUtil.getLinuxNewWin() + " " + sh.getAbsolutePath());
        }
	}
	
	/**
	 * execute your command line jar without redesigning your program to use java.util.Scanner to take input
	 * @since 2.0.0-rc.7
	 */	
	public static String[] wrapWithCMD(String[] argsInit)
	{
		return wrapWithCMD("", argsInit);
	}
	
	/**
	 * execute your command line jar without redesigning your program to use java.util.Scanner to take input.
	 * escape sequences are \char to have actual quotes in the jvm args cross platform
	 * @since 2.0.0-rc.8
	 */	
	public static String[] wrapWithCMD(String msg, String[] argsInit)
	{
		Class<?> mainClass = getMainClass();
		String appId = mainClass.getName().replaceAll("\\.", "/");
		return wrapWithCMD(msg, appId, appId, mainClass, argsInit);
	}
	
	/**
	 * execute your command line jar without redesigning your program to use java.util.Scanner to take input.
	 * escape sequences are \char to have actual quotes in the jvm args cross platform
	 * @since 2.0.0-rc.8
	 */		
	public static String[] wrapWithCMD(String msg, String appId, String appName, String[] argsInit)
	{
		return wrapWithCMD(msg, appId, appName, getMainClass(), argsInit);
	}
	
	/**
	 * execute your command line jar without redesigning your program to use java.util.Scanner to take input.
	 * escape sequences are \char to have actual quotes in the jvm args cross platform
	 * @since 2.0.0-rc.8
	 */	
	public static String[] wrapWithCMD(String msg, String appId, String appName, Class<?> mainClass, String[] argsInit)
	{
		SelfCommandPrompt.runWithCMD(appId, appName, mainClass, argsInit, false, true);
		boolean shouldScan = argsInit.length == 0;
		if(!msg.isEmpty() && shouldScan)
			System.out.println(msg);
		String[] newArgs = shouldScan ? parseCommandLine() : argsInit;
		if(isEmpty(newArgs, true))
			newArgs = new String[0];//if args are empty from the user simulate it
		return newArgs;
	}

	private static String[] parseCommandLine() 
	{
		String[] args = Command.fixArgs(split(Command.getScanner().nextLine(), ' ', '"', '"'));
		List<String> list = Arrays.asList(args);
		return (String[]) list.toArray();
	}

	/**
	 * returns if the entire array is empty and whether or not to trim it before hand
	 */
	public static boolean isEmpty(String[] arr, boolean trim) 
	{
		for(String s : arr)
		{
			s = trim ? s.trim() : s;
			if(!s.isEmpty())
				return false;
		}
		return true;
	}

	public static void shutdown()
	{
		System.gc();
		System.exit(0);
	}

	/**
	 * checks if the jar is compiled based on the main class 
	 */
	public static boolean isCompiled()
	{
		return isCompiled(getMainClass());
	}
	
	/**
	 * checks per class if the jar is compiled
	 */
	public static boolean isCompiled(Class<?> mainClass)
	{
		try 
		{
			File file = getFileFromClass(mainClass);
			return getExtension(file).equals("jar") || getMainClassName().endsWith("jarinjarloader.JarRsrcLoader");
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * get a file from a class
	 */
	public static File getFileFromClass(Class<?> clazz) throws UnsupportedEncodingException, RuntimeException
	{
		String jarPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();//get the path of the currently running jar
		String fileName = URLDecoder.decode(jarPath, "UTF-8").substring(1);
		if(fileName.contains(INVALID))
			throw new RuntimeException("jar file contains invalid parsing chars:" + fileName);
		return new File(fileName);
	}
	
	public static List<String> getJVMArgs()
	{
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		return runtimeMxBean.getInputArguments();
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
	
	public static String[] programArgs(String[] args) 
	{
		for(int i=0;i<args.length; i++)
		{
			args[i] = "\"" + args[i] + "\"";
		}
		return args;
	}
	
	/**
	 * incompatible with eclipse's jar in jar loader. Use this to enforce your program's directory is synced with your jar after calling runWithCMD
	 */
	public static void syncUserDirWithJar()
	{
		try 
		{
			setUserDir(getFileFromClass(getMainClass()).getParentFile());
		}
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void setUserDir(File file)
	{
		System.setProperty("user.dir", file.getAbsolutePath());
	}
	
	//Start APP VARS_____________________________
	
	/**
	 * @return if the main class is SelfCommandPrompt
	 * @Since 2.0.0-rc.6
	 */
	public static boolean isWrapped() 
	{
		return SelfCommandPrompt.class.getName().equals(getMainClassName());
	}
	
	/**
	 * returns the appdata contained in %appdata%/SelfCommandPrompt/appId
	 */
	public static File getAppdata(String appId)
	{
		return new File(selfcmd, appId);
	}
	
	public static boolean hasJConsole() 
	{
		return useJConsole || OSUtil.isUnsupported();
	}

	public static String terminal;
	public static boolean useJConsole;
	/**
	 * syncs the global terminal and the boolean for useJConsole
	 */
	public static void syncConfig() 
	{
    	MapConfig cfg = new MapConfig(new File(selfcmd, "console.cfg"));
    	cfg.load();
    	
    	//load the terminal string
    	String cfgTerm = cfg.get("terminal", "").trim();
    	if(cfgTerm.isEmpty())
    	{
    		cfgTerm = OSUtil.getTerminal();//since it's a heavy process cache it to the config
    		cfg.set("terminal", cfgTerm);
    	}
    	terminal = cfgTerm;
    	
    	useJConsole= cfg.get("useJConsole", false);//if user prefers JConsole over natives
    	cfg.save();
	}
	
	public static void cacheApp(String appId, String appName, Class<?> mainClass, String[] args, boolean pause) 
	{
		wrappedAppId = appId;
		wrappedAppName = appName;
		wrappedAppClass = SelfCommandPrompt.class.equals(mainClass) ? wrappedAppClass : mainClass;
		wrappedAppArgs = args;
		wrappedPause = pause;
	}

	//End APP VARS_________________________________
	
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
	
	/**
	 * split with quote ignoring support
	 */
	public static String[] split(String str, char sep, char lquote, char rquote) 
	{
		if(str.isEmpty())
			return new String[]{str};
		List<String> list = new ArrayList<>();
		boolean inside = false;
		for(int i = 0; i < str.length(); i += 1)
		{
			String a = str.substring(i, i + 1);
			String prev = i == 0 ? "a" : str.substring(i-1, i);
			boolean escape = prev.charAt(0) ==  '\\';
			if(a.equals("" + lquote) && !escape || a.equals("" + rquote) && !escape)
			{
				inside = !inside;
			}
			if(a.equals("" + sep) && !inside)
			{
				String section = str.substring(0, i);
				list.add(section);
				str = str.substring(i + ("" + sep).length(), str.length());
				i = -1;
			}
		}
		list.add(str);//add the rest of the string
		return toArray(list, String.class);
	}
	
	public static <T> T[] toArray(Collection<T> col, Class<T> clazz)
	{
	    @SuppressWarnings("unchecked")
		T[] li = (T[]) Array.newInstance(clazz, col.size());
	    int index = 0;
	    for(T obj : col)
	    {
	        li[index++] = obj;
	    }
	    return li;
	}
	
	public static boolean containsAny(String string, String invalid) 
	{
		if(string.isEmpty())
			return invalid.isEmpty();
		
		for(int i=0; i < string.length(); i++)
		{
			String s = string.substring(i, i + 1);
			if(invalid.contains(s))
			{
				return true;
			}
		}
		return false;
	}
}