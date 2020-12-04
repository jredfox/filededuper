package jredfox.filededuper;

import java.io.File;

import jredfox.filededuper.command.Command;
import jredfox.filededuper.config.simple.MapConfig;
import jredfox.selfcmd.SelfCommandPrompt;

public class Main {
	
	public static final String VERSION = "0.14.3";
	public static final String appName = "File De-Duper " + VERSION;
	public static final String appId = "File-De-Duper";
	
	public static void main(String[] args)
	{
		args = SelfCommandPrompt.wrapWithCMD("", "filededuper", "a test", Main.class, args, true, true);
//		SelfCommandPrompt.runWithCMD(appId, appName, args);
		loadConfigs();
		System.out.println("Starting " + appName);
		if(args.length != 0)
		{
			Command.run(args);
		}
		else
		{
			do
			{
				Command.run(Command.nextValidCommand());
			}
			while(true);
		}
	}

	//file deduper config
	public static String[] genExt;
	public static String[] genDupesExt;
	public static String[] compareExt;
	public static HashType compareHash;
	public static boolean lowercaseHash;
	public static boolean skipGenPluginData;
	
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
		genExt = mainCfg.get("genHashesExtension", "*").toLowerCase().replace("\\.", "").split(",");
		genDupesExt = mainCfg.get("genDupesExtension", "*").toLowerCase().replace("\\.", "").split(",");
		compareExt = mainCfg.get("compareHashesExtension", "*").toLowerCase().replace("\\.", "").split(",");
		lowercaseHash = mainCfg.get("lowercaseHash", false);
		skipGenPluginData = mainCfg.get("skipGenPluginData", false);
		compareHash = HashType.getByName(mainCfg.get("compareHash", "MD5").toLowerCase().replace("-", ""));//options are MD5, SHA1, SHA256
		mainCfg.save();
		
		MapConfig jarCheck = new MapConfig(new File(getProgramDir(), "checkJar.cfg"));
		jarCheck.load();
		archiveDir = jarCheck.get("archiveDir", false);
		compileTimePoints = jarCheck.get("compileTimePoints", true);//getCompile time based on maximum points based on the program dir then the lowest one in there
		compileTimeOffset = jarCheck.get("compileTimeOffset", 30);//amount of minuets is allowed since the compile time before it's considered a mod
		checkJarSigned = jarCheck.get("checkJarSigned", true);
		consistentCheckJar = jarCheck.get("consistentCheckJar", false);
		programExts = jarCheck.get("programExts", "class,rsa,dsa,mf,sf,js,java,py,kt").toLowerCase().replace("\\.", "").split(",");
		programDirs = getProgramDirs(jarCheck);
		jarCheck.save();
	}
	
	public static enum HashType
	{
		MD5(32),
		SHA1(40),
		SHA256(64);
		
		public int size;
		HashType(int l)
		{
			this.size = l;
		}
		
		public static HashType getByName(String name)
		{
			for(HashType t : HashType.values())
			{
				if(t.toString().toLowerCase().equals(name))
				{
					return t;
				}
			}
			return null;
		}
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
