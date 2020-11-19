package jredfox.filededuper.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import jredfox.filededuper.Main;
import jredfox.filededuper.PointTimeEntry;
import jredfox.filededuper.archive.ArchiveEntry;
import jredfox.filededuper.archive.Zip;
import jredfox.filededuper.util.JarUtil.Consistencies;

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
				String hashName = getTrueName(f) + "-" + md5 + getExtensionFull(f);
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
	
	public static String getTrueName(File file)
	{
		String name = file.getName();
		if(file.isDirectory() || !name.contains("."))
			return name;
		return name.substring(0, name.toLowerCase().indexOf(getExtension(file)) - 1);//name must be lower case in case the extension is actually uppercase inside the filename
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
		return name.contains(".") ? name.split("\\.")[name.split("\\.").length - 1].toLowerCase() : "";
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
			return getMD5(new FileInputStream(f));
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
			return getSHA256(new FileInputStream(f));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getMD5(InputStream input) throws IOException
	{
		String hash = DigestUtils.md5Hex(input);
		if(!Main.lowercaseHash)
			hash = hash.toUpperCase();
		input.close();
		return hash;
	}
	
	/**
	 * closes inputstream
	 */
	public static String getSHA256(InputStream input) throws IOException
	{
		String hash = DigestUtils.sha256Hex(input);
		if(!Main.lowercaseHash)
			hash = hash.toUpperCase();
		input.close();
		return hash;
	}
	

	public static boolean isWithinRange(long min, long max, long num) 
	{
		return num >= min && num <= max;
	}
	
	/**
	 * an optimized way to split a string from it's first instanceof a char
	 */
	public static String[] splitFirst(String s, char reg)
	{
		String[] parts = new String[2];
		for(int i=0;i<s.length();i++)
		{
			char c = s.charAt(i);
			if(c == reg)
			{
				parts[0] = s.substring(0, i);
				parts[1] = s.substring(i + 1, s.length());
				break;
			}
		}
		if(parts[0] == null)
			return new String[]{s};
		return parts;
	}
	
	public static String parseQuotes(String s, int index, String q) 
	{
		if(index == -1)
			return "";
		char lquote = q.charAt(0);
		char rquote = q.length() > 1 ? q.charAt(1) : lquote;
		
		String strid = "";
		int quote = 0;
		for(int i=index;i<s.length();i++)
		{
			if(quote == 2)
				break; //if end of parsing object stop loop and return getParts(strid,":");
			char tocompare = s.charAt(i);
			boolean contains = tocompare == lquote && quote == 0 || tocompare == rquote;
			
			if(contains)
				quote++;
			if(!contains && quote > 0)
				strid += tocompare;
		}
		return strid;
	}

	/**
	 * prevents ArrayIndexOutOfBoundsException
	 */
	public static <T> T getSafley(List<T> entries, int index) 
	{
		if(DeDuperUtil.isWithinRange(entries.isEmpty() ? index + 1 : 0, entries.size() - 1, index))
			return entries.get(index);
		return null;
	}
	
	public static boolean endsWith(String str, String[] exts)
	{
		String orgExt = str.toLowerCase();
		for(String ext : exts)
		{
			if(orgExt.endsWith(ext.toLowerCase()))
				return true;
		}
		return false;
	}
	
	public static String getMD5(ZipFile zip, ZipEntry entry)
	{
		try
		{
			return DeDuperUtil.getMD5(new ByteArrayInputStream(JarUtil.extractInMemory(zip, entry)));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getSHA256(ZipFile zip, ZipEntry entry)
	{
		try 
		{
			return DeDuperUtil.getSHA256(new ByteArrayInputStream(JarUtil.extractInMemory(zip, entry)));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean isProgramExt(String name)
	{
		return DeDuperUtil.endsWith(name, Main.programExts);
	}
	
	public static void genMD5s(File dir, File file, Set<String> md5s, List<String> list)
	{
		boolean isJar = getExtension(file).equals("jar");
		if(isJar)
			genJarMD5s(dir, file, md5s, list);
		else
			genFileMD5s(dir, file, md5s, list);//if I really felt like it I could make plugins for each extension that other people create
	}

	private static void genFileMD5s(File dir, File file, Set<String> md5s, List<String> list) 
	{
		String name = file.getName();
		String md5 = DeDuperUtil.getMD5(file);
		if(md5s.contains(md5))
			return;
		String sha256 = DeDuperUtil.getSHA256(file);
		long time = file.lastModified();
		String path = DeDuperUtil.getRealtivePath(dir.isDirectory() ? dir : dir.getParentFile(), file);
		list.add(name + "," + md5 + "," + sha256 + "," + time + "," + path);
		md5s.add(md5);
	}

	private static void genJarMD5s(File dir, File file, Set<String> md5s, List<String> list)
	{
		String name = file.getName();
		String md5 = DeDuperUtil.getMD5(file);
		if(md5s.contains(md5))
			return;
		String sha256 = DeDuperUtil.getSHA256(file);
		long time = file.lastModified();
		Zip jar = JarUtil.getZipFile(file);
		List<ZipEntry> entries = JarUtil.getZipEntries(jar);
		long compileTime = JarUtil.getCompileTime(entries);
		boolean modified = JarUtil.isJarModded(jar.file, entries, Main.checkJarSigned);
		JarUtil.Consistencies consistency = JarUtil.getConsistentcy(entries);
		String path = DeDuperUtil.getRealtivePath(dir.isDirectory() ? dir : dir.getParentFile(), file);
		list.add(name + "," + md5 + "," + sha256 + "," + time + "," + compileTime + "," + modified + "," + consistency + "," + path);
		md5s.add(md5);
		IOUtils.close(jar, true);
	}

	public static String caseString(String string, boolean lowerCase) 
	{
		return lowerCase ? string.toLowerCase() : string.toUpperCase();
	}

	public static String toString(String[] args, String sep) 
	{
		if(args == null)
			return null;
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(String s : args)
		{
			b.append(index + 1 != args.length ? s + sep : s);
			index++;
		}
		return b.toString();
	}

}
