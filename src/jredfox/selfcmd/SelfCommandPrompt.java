package jredfox.selfcmd;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import jredfox.filededuper.config.simple.MapConfig;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.selfcmd.cmd.ExeBuilder;
import jredfox.selfcmd.jconsole.JConsole;
import jredfox.selfcmd.util.OSUtil;
/**
 * @author jredfox. Credits to Chocohead#7137 for helping me find the windows start command
 * this class is a wrapper for your program. It fires command prompt and stops it from quitting without user input
 */
public class SelfCommandPrompt {
	
	public static final String VERSION = "2.0.0";
	public static final String INVALID = "\"'`,";
	public static final File selfcmd = new File(OSUtil.getAppData(), "SelfCommandPrompt");
	
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
	 * NOTE: is WIP and doesn't take input currently use shell / batch files for unsupported oses in the mean time to run the jar
	 * supports all platforms no need to reboot, supports debugging and all ides, and supports shutdown hooks
	 */
	public static JConsole startJConsole(String appName)
	{	
		JConsole console = new JConsole(appName)
		{
			@Override
			public boolean isJavaCommand(String[] command){return true;}//always return true we do not support os commands in JConsole

			@Override
			public boolean shutdown(){return true;}
		};
		console.setEnabled(true);
		System.out.println("JCONSOLE isn't working yet. Please check back in a future version ;)");
		return console;
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
		boolean compiled = isCompiled(mainClass);
		if(!compiled && onlyCompiled || compiled && System.console() != null || isDebugMode() || SelfCommandPrompt.class.getName().equals(getMainClassName()))
		{
			return;
		}
        
		syncConfig(appId);
        if(hasJConsole())
        {
        	startJConsole(appName);
        	return;
        }
		rebootWithTerminal(mainClass, args, appName, appId, false, pause);
	}

	/**
	 * do not call this directly without checks or it will just keep rebooting.
	 * Make sure that the config is synced before calling this directly.
	 * Will reboot the program even it's just with JConsole
	 */
	public static void rebootWithTerminal(Class<?> mainClass, String[] args, String appName, String appId, boolean sync, boolean pause)
	{
    	if(DeDuperUtil.containsAny(appId, INVALID))
    		throw new RuntimeException("appId contains illegal parsing characters:(" + appId + "), invalid:" + INVALID);
        try
        {
            String libs = System.getProperty("java.class.path");
            if(DeDuperUtil.containsAny(libs, INVALID))
            	throw new RuntimeException("one or more LIBRARIES contains illegal parsing characters:(" + libs + "), invalid:" + INVALID);
            
            if(sync)
            	syncConfig(appId);
            
            if(hasJConsole())
            {
                ExeBuilder builder = new ExeBuilder();
                builder.addCommand(terminal);
                builder.addCommand(OSUtil.getExeAndClose());
            	builder.addCommand("java");
            	builder.addCommand(getJVMArgs());
            	builder.addCommand("-cp");
            	builder.addCommand("\"" + libs + "\"");
            	builder.addCommand(mainClass.getName());
            	builder.addCommand(programArgs(args));
            	String command = builder.toString();
        		Runtime.getRuntime().exec(command);
        		shutdown();
            }
            else
            {
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
            	runInNewTerminal(appName, appId, appId, command);
            	shutdown();
            }
        }
        catch (Exception e)
        {	
			SelfCommandPrompt.startJConsole(appName);//use JConsole as a backup in case they are on a very old os version
        	e.printStackTrace();
		}
	}
	
	/**
	 * runs a command in a new terminal window.
	 * the sh name is the file name you want the shell script stored. The appId is to locate your folder
	 */
	public static void runInNewTerminal(String appName, String appId, String shName, String command) throws IOException
	{
        if(OSUtil.isWindows())
        {
        	Runtime.getRuntime().exec(terminal + " /c start " + "\"" + appName + "\" " + command);//power shell isn't supported as it screws up with the java -cp command when using the gui manually
        }
        else if(OSUtil.isMac())
        {
        	File sh = new File(getAppdata(appId), shName + ".sh");
        	List<String> cmds = new ArrayList<>();
        	cmds.add("#!/bin/bash");
        	cmds.add("set +v");
        	cmds.add("echo -n -e \"\\033]0;" + appName + "\\007\"");
        	cmds.add("cd " + getProgramDir().getAbsolutePath());//enforce same directory with mac's redirects you never know where you are
        	cmds.add(command);
        	IOUtils.saveFileLines(cmds, sh, true);
        	IOUtils.makeExe(sh);
        	Runtime.getRuntime().exec("/bin/bash -c " + "osascript -e \"tell application \\\"Terminal\\\" to do script \\\"" + sh.getAbsolutePath() + "\\\"\"");
        }
        else if(OSUtil.isLinux())
        {
        	Runtime.getRuntime().exec(terminal + " -x " + "--title=" + "\"" + appName + "\" " + command);//use the x flag to enforce it in the new window
        }
	}

	public static String terminal;
	public static boolean useJConsole;
	/**
	 * configurable per app
	 */
	public static void syncConfig(String appId) 
	{
    	MapConfig cfg = new MapConfig(new File(getAppdata(appId), "console.cfg"));
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
	
	/**
	 * returns the appdata contained in %appdata%/SelfCommandPrompt/appId
	 */
	private static File getAppdata(String appId)
	{
		return new File(selfcmd, appId);
	}
	
	public static boolean hasJConsole() 
	{
		return useJConsole || OSUtil.isUnsupported();
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
	
	public static String getJVMArgsAsString()
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