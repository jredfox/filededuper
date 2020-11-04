package jredfox.filededuper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.Files;

public class Main {
	
	public static boolean errored;
	
	public static void main(String[] args)
	{
		Scanner scanner = new Scanner(System.in);
		System.out.println("move all files to one directory T/F");
		boolean is1D = scanner.nextLine().substring(0, 1).toLowerCase().equals("t");
		System.out.println("input folder to de-dupe:");
		File input = new File(scanner.nextLine());
		File output = new File(input.getParent(), input.getName() + "-output");
		List<File> files = getDirFiles(input, new String[]{"*"});
		if(is1D)
		{
			move1D(files, output);
		}
		else
		{
			move2D(files, input, output);
		}
		scanner.close();
		System.out.println("finished " + (errored ? "with errors" : "successfully"));
	}
	
	public static void move1D(List<File> files, File dir)
	{
		Set<String> hashes = getMD5S(dir);
		for(File f : files)
		{
			try
			{
				String md5 = getMD5(f);
				if(hashes.contains(md5))
				{
					System.out.println("skipping dupe:" + md5 + ", " + f);
					continue;
				}
				File output = new File(dir, getFileTrueName(f) + "-" + md5 + "." + getExtension(f));
				output.getParentFile().mkdirs();
				Files.copy(f, new FileOutputStream(output));
				hashes.add(md5);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				errored = true;
			}
		}
	}
	
	public static void move2D(List<File> files, File input, File outputDir)
	{
		Set<String> hashes = getMD5S(outputDir);
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
				File outputFile = new File(outputDir, getRealtivePath(input, f));
				if(outputFile.exists())
				{
					outputFile = new File(outputFile.getParent(), getFileTrueName(f) + "-" + md5 + "." + getExtension(f));
					System.out.println("avoiding overwriting file:" + outputFile);
				}
				outputFile.getParentFile().mkdirs();
				Files.copy(f, new FileOutputStream(outputFile));
				hashes.add(md5);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				errored = true;
			}
		}
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

	public static Set<String> getMD5S(File dir)
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
	
	public static List<File> getDirFiles(File dir, String... ext)
	{
		List<File> list = new ArrayList<>(dir.listFiles().length + 10);
		getDirFiles(list, dir, ext, false);
		return list;
	}
	
	public static void getDirFiles(List<File> files, File dir, String[] exts, boolean blackList) 
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
	
	public String getSHA256(File f)
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

}
