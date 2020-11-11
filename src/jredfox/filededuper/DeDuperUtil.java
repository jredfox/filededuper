package jredfox.filededuper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.Files;

public class DeDuperUtil {
	
	public static void move(List<File> files, File input, File outputDir, boolean sameDir)
	{
		Set<String> hashes = getHashes(outputDir);
		for(File f : files)
		{
			try
			{
				String md5 = getMD5(f);
				if(hashes.contains(md5))
				{
					System.out.println("skipping dupe file:" + md5 + ", " + f);
					continue;
				}
				String hashName = getFileTrueName(f) + "-" + md5 + getExtensionFull(f);
				File outputFile = sameDir ? (new File(outputDir,  hashName)) : new File(outputDir, getRealtivePath(input, f));
				if(outputFile.exists())
				{
					outputFile = new File(outputFile.getParent(), hashName);
					System.out.println("avoiding overwriting file:" + outputFile);
				}
				copy(f, outputFile);
				hashes.add(md5);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Main.errored = true;
			}
		}
	}
	
	/**
	 * copy a file. preserve date modified, and create dirs if non existent
	 */
	public static void copy(File input, File output) throws FileNotFoundException, IOException
	{
		output.getParentFile().mkdirs();
		Files.copy(input, new FileOutputStream(output));
		output.setLastModified(input.lastModified());
	}
	
	public static String getFileTrueName(File file)
	{
		if(!file.getName().contains("."))
			return file.getName();
		return file.getName().substring(0, file.getName().indexOf(getExtension(file)) - 1);
	}
	
	public static String getRealtivePath(File dir, File file) 
	{
		String fpath = file.getPath();
		return fpath.substring(dir.getPath().indexOf(dir.getPath()) + dir.getPath().length() + 1, fpath.length());
	}

	public static Set<String> getHashes(File dir)
	{
		if(!dir.exists())
			return new HashSet<>(0);
		Set<String> list = new HashSet<>();
		for(File f : getDirFiles(dir, "*"))
		{
			list.add(getMD5(f));
		}
		return list;
	}
	
	/**
	 * get a list of files from a file or directory
	 */
	public static List<File> getDirFiles(File dir, String... exts)
	{
		return getDirFiles(dir, exts, false);
	}
	
	/**
	 * get a list of files from a file or directory. has blacklist extension support
	 */
	public static List<File> getDirFiles(File dir, String[] exts, boolean blackList) 
	{
		if(!dir.isDirectory())
		{
			List<File> li = new ArrayList(1);
			li.add(dir);
			return li;
		}
		List<File> list = new ArrayList<>(dir.listFiles().length + 10);
		getDirFiles(list, dir, exts, false);
		return list;
	}
	
	protected static void getDirFiles(List<File> files, File dir, String[] exts, boolean blackList) 
	{
		if(blackList && exts[0].equals("*"))//this says to blacklist any file so don't do anything
			return;
		
	    for (File file : dir.listFiles()) 
	    {
	    	String extension = getExtension(file);
	    	boolean isType = blackList ? (!contains(exts, extension)) : (exts[0].equals("*") || contains(exts, extension));
	        if (file.isFile() && isType)
	        {
	            files.add(file);
	        }
	        else if (file.isDirectory()) 
	        {
	        	getDirFiles(files, file, exts, blackList);
	        }
	    }
	}
	
	public static String getExtensionFull(File file) 
	{
		String ext = getExtension(file);
		if(!ext.isEmpty())
			ext = "." + ext;
		return ext;
	}

	public static String getExtension(File file) 
	{
		String name = file.getName();
		return name.contains(".") ? name.split("\\.")[name.split("\\.").length - 1] : "";
	}

	public static boolean contains(Object[] arr, Object obj)
	{
		for(Object o : arr)
			if(o.equals(obj))
				return true;
		return false;
	}
	
	public static String getMD5(File f)
	{
		try
		{
			return DigestUtils.md5Hex(new FileInputStream(f)).toUpperCase();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getSHA256(File f)
	{
		try
		{
			return DigestUtils.sha256Hex(new FileInputStream(f)).toUpperCase();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Overwrites entire file default behavior no per line modification removal/addition
	 */
	public static void saveFileLines(List<String> list,File f,boolean utf8)
	{
		BufferedWriter writer = null;
		try
		{
			if(!utf8)
			{
				writer = new BufferedWriter(new FileWriter(f));
			}
			else
			{
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8 ) );
			}
			
			for(String s : list)
			{
				writer.write(s + "\r\n");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(writer != null)
			{
				try
				{
					writer.close();
				}
				catch(Exception e)
				{
					System.out.println("Unable to Close OutputStream this is bad");
				}
			}
		}
	}

}
