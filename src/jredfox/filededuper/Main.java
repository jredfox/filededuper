package jredfox.filededuper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.Files;

import jredfox.filededuper.command.CMDNotFoundException;
import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;

public class Main {
	
	public static boolean errored;
	public static final String VERSION = "0.3.1";
	
	public static void main(String[] programArgs)
	{
		loadConfig();
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

	public static long time;
	public static String genExt;
	public static String compareExt;
	public static boolean addedCompareDir;
	
	public static void loadConfig() 
	{
		MapConfig cfg = new MapConfig(new File(getProgramDir(), "filededuper.cfg"));
		cfg.load();
		time = cfg.get("maxCompileTimeOffset", 30L);
		genExt = cfg.get("genMD5Extension","*").toLowerCase();
		compareExt = cfg.get("compareMD5Extension","*").toLowerCase();
		addedCompareDir = cfg.get("addedCompareDir", false);
		cfg.save();
	}
	
	public static File getProgramDir()
	{
		return new File(System.getProperty("user.dir"));
	}

}
