package jredfox.filededuper;

import java.io.File;

import jredfox.filededuper.command.CMDNotFoundException;
import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;

public class Main {
	
	public static boolean errored;
	public static final String VERSION = "0.4.3";
	
	public static void main(String[] programArgs)
	{
		loadConfigs();
		if(programArgs.length != 0)
		{
			Command cmd = Commands.getCommand(programArgs[0]);
			if(cmd == null)
				throw new CMDNotFoundException("Command not found Exception:" + programArgs[0]);
			Object[] cmdArgs = cmd.getParams(programArgs);
			long ms = System.currentTimeMillis();
			cmd.run(cmdArgs);
			System.out.println("finished " + (errored ? "with errors" : "successfully in ") + (System.currentTimeMillis() - ms) + "ms");
		}
	}
	
	//file deduper config
	public static String genExt;
	public static String compareExt;
	
	//checkJar config
	public static boolean archiveDir;
	public static boolean compileTimePoints;
	public static long compileTimeOffset;
	public static boolean checkJarSigned;
	public static String[] programExts;
	public static String[] programDirs;
	
	public static void loadConfigs() 
	{
		MapConfig mainCfg = new MapConfig(new File(getProgramDir(), "filededuper.cfg"));
		mainCfg.load();
		genExt = mainCfg.get("genMD5Extension","*").toLowerCase();
		compareExt = mainCfg.get("compareMD5Extension","*").toLowerCase();
		mainCfg.save();
		
		MapConfig jarCheck = new MapConfig(new File(getProgramDir(), "checkJar.cfg"));
		jarCheck.load();
		archiveDir = jarCheck.get("archiveDir", false);
		compileTimePoints = jarCheck.get("compileTimePoints", true);//getCompile time based on maximum points based on the program dir then the lowest one in there
		compileTimeOffset = jarCheck.get("compileTimeOffset", 30);//amount of minuets is allowed since the compile time before it's considered a mod
		checkJarSigned = jarCheck.get("checkJarSigned", true);
		programExts = jarCheck.get("programExts", "class,rsa,dsa,mf,sf,jar").toLowerCase().trim().split(",");
		programDirs = getProgramDirs(jarCheck);
		jarCheck.save();
	}
	
	private static String[] getProgramDirs(MapConfig jarCheck) 
	{
		String[] dirs = jarCheck.get("programDirs", PointTimeEntry.defaultDir + ",net/minecraft,com/mojang,com/a,jredfox/filededuper").split(",");
		int index = 0;
		for(String s : dirs)
			dirs[index++] = s.trim();//repair for user friendlyness
		return dirs;
	}

	public static File getProgramDir()
	{
		return new File(System.getProperty("user.dir"));
	}

}
