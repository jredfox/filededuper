package jredfox.filededuper.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.Files;

import jredfox.filededuper.Main;
import jredfox.filededuper.Main.HashType;
import jredfox.filededuper.archive.Zip;

public class DeDuperUtil {
	
	public static void move(List<File> files, File input, File outputDir, boolean sameDir)
	{
		Set<String> hashes = getHashes(outputDir);
		for(File f : files)
		{
			try
			{
				String hash = DeDuperUtil.getCompareHash(f);
				if(hashes.contains(hash))
				{
					System.out.println("skipping dupe file:" + hash + ", " + f);
					continue;
				}
				String hashName = getTrueName(f) + "-" + hash + getExtensionFull(f);
				File outputFile = sameDir ? (new File(outputDir,  hashName)) : new File(outputDir, getRealtivePath(input, f));
				if(outputFile.exists())
				{
					outputFile = new File(outputFile.getParent(), hashName);
					System.out.println("avoiding overwriting file:" + outputFile);
				}
				copy(f, outputFile);
				hashes.add(hash);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static File newFile(String str)
	{
		File file = new File(str);
		return file.exists() ? file.getAbsoluteFile() : file;
	}
	
	public static File newFile(String dir, String path)
	{
		File file = new File(dir, path);
		return file.exists() ? file.getAbsoluteFile() : file;
	}
	
	public static File newFile(File dir, String path)
	{
		File file = new File(dir, path);
		return file.exists() ? file.getAbsoluteFile() : file;
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
		if(file.isDirectory())
			return name;
		int index = name.lastIndexOf('.');
		return index != -1 ? name.substring(0, index) : name;
	}
	
	public static String getRealtivePath(File dir, File file) 
	{
		String path = file.getPath();
		String dirPath = dir.getPath();
		boolean plus1 = path.contains(File.separator);
		return path.substring(path.indexOf(dirPath) + dirPath.length() + (plus1 ? 1 : 0));
	}

	public static Set<String> getHashes(File dir)
	{
		if(!dir.exists())
			return new HashSet<>(0);
		List<File> files = getDirFiles(dir);
		Set<String> hashes = new HashSet<>(files.size());
		for(File f : files)
			hashes.add(getCompareHash(f));
		return hashes;
	}
	
	public static List<File> getDirFiles(File dir)
	{
		return getDirFiles(dir, "*");
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
			List<File> li = new ArrayList<>(1);
			String ext = getExtension(dir);
			boolean isType = blackList ? !isExtEqual(ext, exts) : isExtEqual(ext, exts);
			if(isType)
				li.add(dir);
			return li;
		}
		List<File> list = new ArrayList<>(dir.listFiles().length + 10);
		getDirFiles(list, dir, exts, false);
		return list;
	}
	
	protected static void getDirFiles(List<File> files, File dir, String[] exts, boolean blackList) 
	{
	    for (File file : dir.listFiles()) 
	    {
	    	String extension = getExtension(file);
	    	boolean isType = blackList ? !isExtEqual(extension, exts) : isExtEqual(extension, exts);
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
		return ext.isEmpty() ? "" : "." + ext;
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
	
	/**
	 * no directory support use at your own risk
	 */
	public static String getExtension(String name) 
	{
		int index = name.lastIndexOf('.');
		return index != -1 ? name.substring(index + 1) : "";
	}
	
	public static void validateHash(String hash) 
	{
		if(hash.length() != Main.compareHash.size)
		{
			throw new RuntimeException("invalid hash size for: " + hash + " expected:" + Main.compareHash.size + " on hashtype:" + Main.compareHash);
		}
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
	
	public static String getSHA1(File f)
	{
		try
		{
			return getSHA1(new FileInputStream(f));
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
	
	public static String getSHA1(InputStream input) throws IOException
	{
		String hash = DigestUtils.sha1Hex(input);
		if(!Main.lowercaseHash)
			hash = hash.toUpperCase();
		input.close();
		return hash;
	}
	

	public static boolean isWithinRange(long min, long max, long num) 
	{
		return num >= min && num <= max;
	}
	
	public static String parseQuotes(String s, char lq, char rq) 
	{
		return parseQuotes(s, 0, lq, rq);
	}

	public static String parseQuotes(String s, int index, char lq, char rq)
	{
		StringBuilder builder = new StringBuilder();
		char prev = 'a';
		int count = 0;
		boolean hasQuote = hasQuote(s.substring(index, s.length()), lq);
		for(int i=index;i<s.length();i++)
		{
			String c = s.substring(i, i + 1);
			char firstChar = c.charAt(0);
			if(firstChar == '\\' && prev == '\\')
			{
				prev = '/';
				firstChar = '/';//escape the escape
			}
			boolean escaped = prev == '\\';
			if(hasQuote && !escaped && (count == 0 && c.equals("" + lq) || count == 1 && c.equals("" + rq)))
			{
				count++;
				if(count == 2)
					break;
				prev = firstChar;//set previous before skipping
				continue;
			}
			if(!hasQuote || count == 1)
			{
				builder.append(c);
			}
			prev = firstChar;//set the previous char here
		}
		return lq == rq ? builder.toString().replaceAll("\\\\" + lq, "" + lq) : builder.toString().replaceAll("\\\\" + lq, "" + lq).replaceAll("\\\\" + rq, "" + rq);
	}

	public static boolean hasQuote(String str, char lq) 
	{
		char prev = 'a';
		for(char c : str.toCharArray())
		{
			if(c == lq && prev != '\\')
				return true;
			prev = c;
		}
		return false;
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
	
	public static String getCompareHash(File file)
	{
		return Main.compareHash == HashType.MD5 ? DeDuperUtil.getMD5(file) : Main.compareHash == HashType.SHA1 ?  DeDuperUtil.getSHA1(file) : Main.compareHash == HashType.SHA256 ? DeDuperUtil.getSHA256(file) : null;
	}
	
	public static String getCompareHash(InputStream in) throws IOException
	{
		return Main.compareHash == HashType.MD5 ? DeDuperUtil.getMD5(in) : Main.compareHash == HashType.SHA1 ?  DeDuperUtil.getSHA1(in) : Main.compareHash == HashType.SHA256 ? DeDuperUtil.getSHA256(in) : null;
	}
	
	public static String getCompareHash(ZipFile zip, ZipEntry entry) 
	{
		try
		{
			return DeDuperUtil.getCompareHash(new ByteArrayInputStream(JarUtil.extractInMemory(zip, entry)));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return null;
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
	
	public static String getSHA1(ZipFile zip, ZipEntry entry)
	{
		try
		{
			return DeDuperUtil.getSHA1(new ByteArrayInputStream(JarUtil.extractInMemory(zip, entry)));
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
	
	public static boolean isProgramExt(String filename)
	{
		return DeDuperUtil.isFileExt(filename, Main.programExts);
	}
	
	/**
	 * input the filename.extension here with a list of extensions to return from
	 */
	public static boolean isFileExt(String filename, String... exts) 
	{
		String orgExt = getExtension(filename);
		return isExtEqual(orgExt, exts);
	}

	public static boolean isExtEqual(String orgExt, String... exts)
	{
		orgExt = orgExt.toLowerCase();
		for(String ext : exts)
		{
			if(ext.equals("*") || orgExt.isEmpty() && ext.equals("noextension") || orgExt.equals(ext))
				return true;
		}
		return false;
	}

	public static void genHashes(File dir, File file, Set<String> hashes, List<String> list)
	{
		String hash = DeDuperUtil.getCompareHash(file);
		if(hashes.contains(hash))
			return;
		list.add(genHashes(dir, file, hash));
		hashes.add(hash);
	}
	
	public static void genDupeHashes(File dir, File file, Map<String, File> hashes, List<String> list)
	{
		String hash = DeDuperUtil.getCompareHash(file);
		if(!hashes.containsKey(hash))
		{
			hashes.put(hash, file);
			return;
		}
		list.add(genHashes(dir, file, hash) + "," + DeDuperUtil.getRealtivePath(dir.isDirectory() ? dir : dir.getParentFile(), hashes.get(hash)) );
	}

	private static String genHashes(File dir, File file, String hash) 
	{
		String name = file.getName();
		//recycle the comparing hash so it doesn't generate it twice especially for the sha's hashes
		String md5 = Main.compareHash == HashType.MD5 ? hash : DeDuperUtil.getMD5(file);
		String sha1 = Main.compareHash == HashType.SHA1 ? hash : DeDuperUtil.getSHA1(file);
		String sha256 = Main.compareHash == HashType.SHA256 ? hash : DeDuperUtil.getSHA256(file);
		long size = file.length();
		long time = file.lastModified();
		String plugin = getPlugin(DeDuperUtil.getExtension(file), file);
		String path = DeDuperUtil.getRealtivePath(dir.isDirectory() ? dir : dir.getParentFile(), file);
		return name + "," + md5 + "," + sha1 + "," + sha256 + "," + size + "," + time + (plugin.isEmpty() ? "" : "," + plugin) + "," + path;
	}

	private static String[] pluginExts = new String[]{"jar"};
	private static String getPlugin(String ext, File file) 
	{
		try
		{
			return Main.skipGenPluginData || !isExtEqual(ext, pluginExts) ? "" : genJarData(file);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "exception_" + e.getClass().getName();
		}
	}

	private static String genJarData(File file)
	{
		Zip jar = JarUtil.getZipFile(file);
		List<ZipEntry> entries = JarUtil.getZipEntries(jar);
		long compileTime = JarUtil.getCompileTimeSafley(file, entries);
		boolean modified = JarUtil.isJarModded(jar.file, entries, Main.checkJarSigned);
		JarUtil.Consistencies consistency = JarUtil.getConsistentcy(file, entries, compileTime);
		IOUtils.close(jar, true);
		return compileTime + "," + modified + "," + consistency;
	}

	public static String caseString(String string, boolean lowerCase) 
	{
		return lowerCase ? string.toLowerCase() : string.toUpperCase();
	}

	public static <T extends Object> String toString(T[] args, String sep) 
	{
		if(args == null)
			return null;
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(T obj : args)
		{
			String s = obj.toString();
			b.append(index + 1 != args.length ? s + sep : s);
			index++;
		}
		return b.toString();
	}
	
	public static <T extends Object> String toString(Collection<T> args, String sep) 
	{
		if(args == null)
			return null;
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(T obj : args)
		{
			String s = obj.toString();
			b.append(index + 1 != args.size() ? s + sep : s);
			index++;
		}
		return b.toString();
	}
	
	public static String[] splitFirst(String str, char sep, char lquote, char rquote)
	{
		return split(str, 1, sep, lquote, rquote);
	}
	
	public static String[] split(String str, char sep, char lquote, char rquote) 
	{
		return split(str, -1, sep, lquote, rquote);
	}
	
	/**
	 * split with quote ignoring support
	 * @param limit is the amount of times it will attempt to split
	 */
	public static String[] split(String str, int limit, char sep, char lquote, char rquote) 
	{
		if(str.isEmpty())
			return new String[]{str};
		List<String> list = new ArrayList<>();
		boolean inside = false;
		int count = 0;
		for(int i = 0; i < str.length(); i += 1)
		{
			if(limit != -1 && count >= limit)
				break;
			String a = str.substring(i, i + 1);
			char firstChar = a.charAt(0);
			char prev = i == 0 ? 'a' : str.substring(i-1, i).charAt(0);
			boolean escape = prev == '\\';
			if(firstChar == '\\' && prev == '\\')
			{
				prev = '/';
				firstChar = '/';//escape the escape
			}
			if(!escape && (a.equals("" + lquote) || a.equals("" + rquote)))
			{
				inside = !inside;
			}
			if(a.equals("" + sep) && !inside)
			{
				String section = str.substring(0, i);
				list.add(section);
				str = str.substring(i + ("" + sep).length());
				i = -1;
				count++;
			}
		}
		list.add(str);//add the rest of the string
		return toArray(list, String.class);
	}
	
	/**
	 * the array type cannot be casted out of Object[] use toArray(Collection col, Class clazz) instead
	 */
	public static Object[] toArray(Collection col)
	{
		return toArray(col, Object.class);
	}
	
	public static <T> T[] toArray(Collection<T> col, Class<T> clazz)
	{
	    T[] li = (T[]) Array.newInstance(clazz, col.size());
	    int index = 0;
	    for(T obj : col)
	    {
	        li[index++] = obj;
	    }
	    return li;
	}

	public static Long parseTimeStamp(String init)
	{
		return init.equals("current") ? System.currentTimeMillis() : Long.parseLong(init);
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

	public static <T> List<T> asList(T[] args) 
	{
		ArrayList<T> list = new ArrayList<>(args.length);
		for(T obj : args)
			list.add(obj);
		return list;
	}
	
}
