package jredfox.filededuper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base32InputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.Files;

import jredfox.filededuper.command.CMDNotFoundException;
import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;
import jredfox.filededuper.config.simple.MapConfig;

public class Main {
	
	public static boolean errored;
	public static final String VERSION = "0.3.4";
	
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
	
	public static String genExt;
	public static String compareExt;
	public static long time;
	public static boolean archiveDir;
	public static boolean compileTimePoints;
	public static String[] programDirs;
	
	public static void loadConfig() 
	{
		MapConfig cfg = new MapConfig(new File(getProgramDir(), "filededuper.cfg"));
		cfg.load();
		time = cfg.get("maxCompileTimeOffset", 30L);
		genExt = cfg.get("genMD5Extension","*").toLowerCase();
		compareExt = cfg.get("compareMD5Extension","*").toLowerCase();
		archiveDir = cfg.get("archiveDir", false);
		compileTimePoints = cfg.get("compileTimePoints", true);//fetch compile time algorithm based on most consistent classes in dir of program
		programDirs = cfg.get("programDirs", PointTimeEntry.defaultDir + ",net/minecraft,com/a").split(",");
		int index = 0;
		for(String s : programDirs)
			programDirs[index++] = s.trim();//repair for user friendlyness
		cfg.save();
	}
	
	public static File getProgramDir()
	{
		return new File(System.getProperty("user.dir"));
	}

}
