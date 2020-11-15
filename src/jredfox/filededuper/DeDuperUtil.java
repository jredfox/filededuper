package jredfox.filededuper;

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

import com.google.common.io.Files;

import jredfox.filededuper.zip.ArchiveEntry;

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
	
	public static String getMD5(InputStream input) throws IOException
	{
		return DigestUtils.md5Hex(input).toUpperCase();
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
	
	public static String getSHA256(InputStream input) throws IOException
	{
		return DigestUtils.sha256Hex(input).toUpperCase();
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
	
	/**
	 * Equivalent to Files.readAllLines() but, works way faster
	 */
	public static List<String> getFileLines(File f)
	{
		return getFileLines(getReader(f));
	}
	
	public static List<String> getFileLines(String input) 
	{
		return getFileLines(getReader(input));
	}
	
	public static List<String> getFileLines(BufferedReader reader) 
	{
		List<String> list = null;
		try
		{
			list = new ArrayList();
			String s = reader.readLine();
			
			if(s != null)
			{
				list.add(s);
			}
			
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					list.add(s);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				try 
				{
					reader.close();
				} catch (IOException e) 
				{
					System.out.println("Unable to Close InputStream this is bad");
				}
			}
		}
		return list;
	}
	
	/**
	 * even though it's utf8 writing it's the fastest one I tried 5 different other options from different objects
	 */
	public static BufferedWriter getWriter(File f) throws FileNotFoundException, IOException
	{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
	}
	
	public static BufferedReader getReader(File f)
	{
		 try
		 {
			 return new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
		 }
		 catch(Throwable t)
		 {
			 return null;
		 }
	}
	
	public static BufferedReader getReader(String input)
	{
		return new BufferedReader(new InputStreamReader(DeDuperUtil.class.getClassLoader().getResourceAsStream(input)));
	}

	public static boolean isWithinRange(long min, long max, long num) 
	{
		return num >= min && num <= max;
	}
	
	public static List<ZipEntry> getZipEntries(ZipFile zip) 
	{
		List<ZipEntry> entryList = new ArrayList<>(100);
		Enumeration entries = zip.entries();
		while(entries.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if(entry.isDirectory())
				continue;
			entryList.add(entry);
		}
		return entryList;
	}
	
	public static long getCompileTime(List<ZipEntry> entries)
	{
		return Main.compileTimePoints ? getCompileTimePoints(entries) : getCompileTimeLeast(entries);
	}
	
	/**
	 * gets the most likely compiledTime based on lowest date modified of the class file
	 */
	public static long getCompileTimeLeast(List<ZipEntry> entries)
	{
		long ms = -1;
		ZipEntry compileTime = null;
		for(ZipEntry e : entries)
		{
			if(!e.getName().endsWith(".class"))
				continue;
			long time = e.getTime();
			if(time < ms || ms == -1)
			{
				ms = time;
				compileTime = e;
			}
		}
		System.out.println("compileTimeClass:\t" + compileTime);
		return ms;
	}
	
	/**
	 * gets compile time based on program dir points
	 */
	public static long getCompileTimePoints(List<ZipEntry> entries)
	{
		List<PointTimeEntry> points = new ArrayList<>();
		//compile times in an arraylist of ranges
		for(ZipEntry entry : entries)
		{
			String name = entry.getName();
			if(!name.endsWith(".class"))
				continue;
			long time = entry.getTime();
			boolean added = false;
			for(PointTimeEntry point : points)
			{
				if(point.isWithinRange(name, time))
				{
					point.times.add(time);
					added = true;
					break;
				}
			}
			if(!added)
			{
				PointTimeEntry point = new PointTimeEntry(name, time);
				if(point.programDir != null)
				{
					points.add(point);
				}
			}
		}
		Collections.sort(points, new Comparator<PointTimeEntry>()
				{
					@Override
					public int compare(PointTimeEntry p, PointTimeEntry p2)
					{
						return ((Integer)p.times.size()).compareTo(p2.times.size());
					}
				}
				);
		PointTimeEntry point = points.get(points.size() - 1);
		Collections.sort(point.times);
		return point.times.get(0);
	}
	
	/**
	 * get the byte[] in memory from a zip entry
	 * @throws IOException 
	 */
	public static byte[] extractInMemory(ZipFile zipFile, ZipEntry entry) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream input = zipFile.getInputStream(entry);
		copy(input, out, true);
		return out.toByteArray();
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
	 * is cpu & disk intensive
	 */
	public static boolean isJarModded(File file, boolean signed)
	{
		if(!DeDuperUtil.getExtension(file).equals("jar"))
			return false;
		
		ZipFile zip = null;
		boolean modded = false;
		try
		{
			zip = new ZipFile(file);
			modded = !getModdedFiles(zip).isEmpty() || !checkMetainf(zip, signed);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			closeQuietly(zip);
		}
		return modded;
	}
	
	/**
	 * is cpu & disk intensive
	 */
	public static boolean isJarModded(File file, File orgFile)
	{
		if(!DeDuperUtil.getExtension(file).equals("jar") || !DeDuperUtil.getExtension(orgFile).equals("jar"))
			return false;
		
		ZipFile zip = null;
		ZipFile orgZip = null;
		boolean modded = false;
		try
		{
			zip = new ZipFile(file);
			orgZip = new ZipFile(orgFile);
			modded = !getModdedFiles(zip, orgZip).isEmpty();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			closeQuietly(zip);
			closeQuietly(orgZip);
		}
		return modded;
	}
	
	public static boolean checkMetainf(ZipFile jar, boolean signed) 
	{
		return true;//TODO:
	}
	
	/**
	 * @return if the jar was modded and dumped
	 */
	public static boolean dumpJarMod(File file) throws IOException
	{
		ZipFile zip = null;
		try
		{
			zip = new ZipFile(file);
			List<ArchiveEntry> entries = getModdedFiles(zip);
			if(entries.isEmpty())
				return false;
			saveZip(entries, new File(file.getParent(), DeDuperUtil.getFileTrueName(file) + "-output.zip"));
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			closeQuietly(zip);
		}
		return false;
	}
	
	/**
	 * @return if the jar was modded and dumped
	 */
	public static boolean dumpJarMod(File file, File orgFile)
	{
		ZipFile zip = null;
		ZipFile orgZip = null;
		try
		{
			zip = new ZipFile(file);
			orgZip = new ZipFile(orgFile);
			List<ArchiveEntry> entries = getModdedFiles(zip, orgZip);
			if(entries.isEmpty())
				return false;
			saveZip(entries, new File(file.getParent(), DeDuperUtil.getFileTrueName(file) + "-output.zip"));
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			closeQuietly(zip);
			closeQuietly(orgZip);
		}
		return false;
	}

	/**
	 * check the jar self integrity with itself
	 */
	public static List<ArchiveEntry> getModdedFiles(ZipFile zip)
	{
		List<ArchiveEntry> entriesOut = new ArrayList();
		List<ZipEntry> entries = DeDuperUtil.getZipEntries(zip);
		long compileTime = DeDuperUtil.getCompileTime(entries);
		long maxTime = compileTime + ( (1000L * 60L) * Main.time);
		long minTime = compileTime - ( (1000L * 60L) * Main.time);
		
		for(ZipEntry entry : entries)
		{
			long time = entry.getTime();
			if(time > maxTime)
			{
				entriesOut.add(new ArchiveEntry(zip, entry));
			}
			else if(time < minTime && (entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")))
			{
				entriesOut.add(new ArchiveEntry(zip, entry));
			}
		}
		return entriesOut;
	}
	
	/**
	 * compare jar with the original jar file and output any modifications
	 */
	public static List<ArchiveEntry> getModdedFiles(ZipFile zip, ZipFile orgZip)
	{
		List<ZipEntry> entries = DeDuperUtil.getZipEntries(zip);
		List<ArchiveEntry> entriesOut = new ArrayList();
		for(ZipEntry entry : entries)
		{
			ZipEntry orgEntry = orgZip.getEntry(entry.getName());
			if(orgEntry == null)
			{
				entriesOut.add(new ArchiveEntry(zip, entry, new ZipEntry((Main.archiveDir ? "added/" : "mod/") + entry.getName())));
				continue;
			}
			String md5 = DeDuperUtil.getMD5(zip, entry);
			String orgMd5 = DeDuperUtil.getMD5(orgZip, orgEntry);
			if(!md5.equals(orgMd5))
			{
				entriesOut.add(new ArchiveEntry(zip, entry, new ZipEntry( (Main.archiveDir ? "modified/": "mod/") + entry.getName())));
			}
		}
		//compare jarOrg with jar to detect missing files
		List<ZipEntry> orgEntries = DeDuperUtil.getZipEntries(orgZip);
		for(ZipEntry e : orgEntries)
		{
			if(zip.getEntry(e.getName()) == null)
			{
				entriesOut.add(new ArchiveEntry(orgZip, e, new ZipEntry("removed/" + e.getName())));
			}
		}
		return entriesOut;
	}
	
	public static void saveZip(List<ArchiveEntry> entries, File output)
	{
		ZipOutputStream out = null;
		try
		{
			out = new ZipOutputStream(new FileOutputStream(output));
			for(ArchiveEntry e : entries)
			{
				ZipEntry outEntry = new ZipEntry(e.output.getName());
				outEntry.setTime(e.input.getTime());
				out.putNextEntry(outEntry);
				InputStream stream = e.zip.getInputStream(e.input);//is no redirect assume missing file
				copy(stream, out, false);
				stream.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(out != null)
			{
				try 
				{
					out.close();
				} 
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void copy(InputStream in, OutputStream out, boolean close) throws IOException
	{
		byte[] buffer = new byte[1048576/2];
		int length;
   	 	while ((length = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, length);
		}
   	 	if(close)
   	 	{
   	 		in.close();
   	 		out.close();
   	 	}
	}
	
	public static String getMD5(ZipFile zip, ZipEntry entry)
	{
		try
		{
			return DeDuperUtil.getMD5(new ByteArrayInputStream(DeDuperUtil.extractInMemory(zip, entry)));
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
			return DeDuperUtil.getSHA256(new ByteArrayInputStream(DeDuperUtil.extractInMemory(zip, entry)));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static void closeQuietly(ZipFile zip)
	{
		if(zip != null)
		{
			try
			{
				zip.close();	
			}
			catch(Exception e){}
		}
	}
	
	public static void closeQuietly(InputStream stream)
	{
		if(stream != null)
		{
			try
			{
				stream.close();	
			}
			catch(Exception e){}
		}
	}
	
	public static void closeQuitely(OutputStream stream)
	{
		if(stream != null)
		{
			try
			{
				stream.close();	
			}
			catch(Exception e){}
		}
	}

}
