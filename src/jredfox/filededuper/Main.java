package jredfox.filededuper;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import jredfox.filededuper.command.CMDNotFoundException;
import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.selfcmd.SelfCommandPrompt;

public class Main {
	
	public static boolean errored;
	public static final String VERSION = "0.6.0";
	
	public static void main(String[] programArgs)
	{
		SelfCommandPrompt.runwithCMD(Main.class, programArgs, "File de-duper " + VERSION, false);
		loadConfigs();
		Scanner scanner = new Scanner(System.in);
		String strCmd = programArgs.length > 0 ? programArgs[0] : scanner.nextLine();
		programArgs = programArgs.length > 0 ? programArgs : new String[]{strCmd};
		Command cmd = Commands.getCommand(strCmd);
		if(cmd == null)
			throw new CMDNotFoundException("Invalid command:\"" + strCmd + "\"" + " use the \"help\" command to list the commands");
		Object[] cmdArgs = cmd.getParams(programArgs);
		long ms = System.currentTimeMillis();
		cmd.run(cmdArgs);
		System.out.println("finished " + (errored ? "with errors" : "successfully in ") + (System.currentTimeMillis() - ms) + "ms");
	}
	
	//file deduper config
	public static String[] genExt;
	public static String[] compareExt;
	public static boolean lowercaseHash;
	
	//checkJar config
	public static boolean archiveDir;
	public static boolean compileTimePoints;
	public static long compileTimeOffset;
	public static boolean checkJarSigned;
	public static boolean consistentCheckJar;
	public static String[] programExts;
	public static String[] programDirs;
	
	public static void loadConfigs() 
	{
		MapConfig mainCfg = new MapConfig(new File(getProgramDir(), "filededuper.cfg"));
		mainCfg.load();
		genExt = mainCfg.get("genMD5Extension", "*").toLowerCase().replace("\\.", "").split(",");
		compareExt = mainCfg.get("compareMD5Extension", "*").toLowerCase().replace("\\.", "").split(",");
		lowercaseHash = mainCfg.get("lowercaseHash", false);
		mainCfg.save();
		
		MapConfig jarCheck = new MapConfig(new File(getProgramDir(), "checkJar.cfg"));
		jarCheck.load();
		archiveDir = jarCheck.get("archiveDir", false);
		compileTimePoints = jarCheck.get("compileTimePoints", true);//getCompile time based on maximum points based on the program dir then the lowest one in there
		compileTimeOffset = jarCheck.get("compileTimeOffset", 30);//amount of minuets is allowed since the compile time before it's considered a mod
		checkJarSigned = jarCheck.get("checkJarSigned", true);
		consistentCheckJar = jarCheck.get("consistentCheckJar", false);
		programExts = jarCheck.get("programExts", "class,rsa,dsa,mf,sf").toLowerCase().trim().split(",");
		programDirs = getProgramDirs(jarCheck);
		jarCheck.save();
	}
	
	private static String[] getProgramDirs(MapConfig jarCheck) 
	{
		String[] dirs = jarCheck.get("programDirs", PointTimeEntry.defaultDir + ",net/minecraft,com/mojang,com/a,jredfox/filededuper").split(",");
		int index = 0;
		for(String s : dirs)
			dirs[index++] = s.trim();//repair for user friendliness
		return dirs;
	}

	public static File getProgramDir()
	{
		return new File(System.getProperty("user.dir"));
	}

}
