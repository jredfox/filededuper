package jredfox.filededuper.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jredfox.filededuper.Main;
import jredfox.filededuper.PointTimeEntry;
import jredfox.filededuper.archive.ArchiveEntry;

public class JarUtil {
	
	/**
	 * is cpu & disk intensive
	 */
	public static boolean isJarModded(File file, boolean signed)
	{
		if(!DeDuperUtil.getExtension(file).equals("jar"))
			return false;
		
		JarFile jar = null;
		boolean modded = false;
		try
		{
			jar = new JarFile(file);
			modded = !checkMetainf(jar, signed) || !getModdedFiles(jar).isEmpty();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(jar);
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
			IOUtils.closeQuietly(zip);
			IOUtils.closeQuietly(orgZip);
		}
		return modded;
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
			saveZip(entries, new File(file.getParent(), DeDuperUtil.getTrueName(file) + "-output.zip"));
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(zip);
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
			saveZip(entries, new File(file.getParent(), DeDuperUtil.getTrueName(file) + "-output.zip"));
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(zip);
			IOUtils.closeQuietly(orgZip);
		}
		return false;
	}
	
	/**
	 * check the jar self integrity with itself
	 */
	public static List<ArchiveEntry> getModdedFiles(ZipFile zip)
	{
		List<ArchiveEntry> entriesOut = new ArrayList();
		List<ZipEntry> entries = getZipEntries(zip);
		long compileTime = getCompileTime(entries);
		long maxTime = compileTime + ( (1000L * 60L) * Main.compileTimeOffset);
		long minTime = compileTime - ( (1000L * 60L) * Main.compileTimeOffset);
		
		for(ZipEntry entry : entries)
		{
			long time = entry.getTime();
			if(time > maxTime)
			{
//				System.out.println("compileTime:" + compileTime + "," + maxTime + "," + entry);
				entriesOut.add(new ArchiveEntry(zip, entry));
			}
			else if(time < minTime && DeDuperUtil.endsWith(entry.getName(), Main.programExts))
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
		List<ZipEntry> entries = getZipEntries(zip);
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
		List<ZipEntry> orgEntries = getZipEntries(orgZip);
		for(ZipEntry e : orgEntries)
		{
			if(zip.getEntry(e.getName()) == null)
			{
				entriesOut.add(new ArchiveEntry(orgZip, e, new ZipEntry("removed/" + e.getName())));
			}
		}
		return entriesOut;
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
		if(points.isEmpty())
			throw new RuntimeException("Program Directory for DeDuperUtil#getCompileTimePoints() is not found. Add one to the config");
		PointTimeEntry point = points.get(points.size() - 1);
		Collections.sort(point.times);
		return point.times.get(0);
	}
	
	public static boolean checkMetainf(JarFile jar, boolean signed) throws IOException 
	{
		if(signed)
		{
			ZipEntry dsa = getDSA(jar);
			ZipEntry rsa = getRSA(jar);
			if(!checkSignature(jar, dsa) && !checkSignature(jar, rsa))
			{
//				System.out.println("failed on the DSA/RSA");
				return false;
			}
			
			ZipEntry sf = getSF(jar);
			if(sf == null || !checkManifest(jar, new Manifest(jar.getInputStream(sf)), signed))
			{
//				System.out.println("failed on the SF manifest");
				return false;
			}
		}
		Manifest mani = jar.getManifest();
		return checkManifest(jar, mani, signed);
	}
	
	public static boolean checkSignature(ZipFile zip, ZipEntry sig)
	{
		if(sig == null)
			return false;
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(sig), StandardCharsets.UTF_8));
			String signature = IOUtils.getFileLines(reader).get(0).replaceAll("" + (char)65533, "").trim();
			return !signature.isEmpty();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static boolean checkManifest(ZipFile zip, Manifest mf, boolean signed)
	{
		if(mf == null || mf.getMainAttributes().isEmpty())
		{
			return false;
		}
		if(signed)
		{
			if(mf.getEntries().isEmpty())
				return false;
			List<String> names = new ArrayList(mf.getEntries().entrySet().size());
			for(Map.Entry<String, Attributes> pair : mf.getEntries().entrySet())
			{
				String name = pair.getKey();
				Attributes abb = pair.getValue();
				for(Map.Entry<Object, Object> map : abb.entrySet())
				{
					String att = map.getKey().toString();
					if(att.contains("-Digest"))
						names.add(name);
				}
			}
			List<String> actualNames = new ArrayList();
			List<ZipEntry> entries = getZipEntries(zip);
			for(ZipEntry entry : entries)
			{
				if(entry.getName().startsWith("META-INF/"))
					continue;
				actualNames.add(entry.getName());
			}
			Collections.sort(names);
			Collections.sort(actualNames);
			return names.equals(actualNames);
		}
		return true;
	}
	
	private static ZipEntry getSF(JarFile jar) {
		return DeDuperUtil.getSafley(getEntriesFromDir(jar, "META-INF/", ".sf"), 0);
	}
	
	private static ZipEntry getRSA(JarFile jar) {
		return DeDuperUtil.getSafley(getEntriesFromDir(jar, "META-INF/", ".rsa"), 0);
	}

	private static ZipEntry getDSA(JarFile jar) {
		return DeDuperUtil.getSafley(getEntriesFromDir(jar, "META-INF/", ".dsa"), 0);
	}
	
	public static List<ZipEntry> getEntriesFromDir(ZipFile file, String path, String ext)
	{
		ext = ext.toLowerCase();
		List<ZipEntry> list = new ArrayList(2);
		List<ZipEntry> entries = getZipEntries(file);
		for(ZipEntry e : entries)
		{
			if(e.isDirectory())
				continue;
			String name = e.getName();
			if(name.startsWith(path) && name.toLowerCase().endsWith(ext))
			{
				list.add(e);
			}
		}
		return list;
	}
	
	public static List<ZipEntry> getZipEntries(ZipFile zip) 
	{
		List<ZipEntry> entryList = new ArrayList<>(150);
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
	
	/**
	 * use this when getting file hashes from a ZipFile it's slightly faster somehow using this
	 */
	public static byte[] extractInMemory(ZipFile zipFile, ZipEntry entry) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream input = zipFile.getInputStream(entry);
		IOUtils.copy(input, out, true);
		return out.toByteArray();
	}
	
	public static List<ArchiveEntry> getArchiveEntries(ZipFile zip, List<ZipEntry> entries)
	{
		List<ArchiveEntry> archs = new ArrayList(entries.size());
		for(ZipEntry e : entries)
			archs.add(new ArchiveEntry(zip, e));
		return archs;
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
				IOUtils.copy(stream, out, false);
				stream.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(out);
		}
	}

	public static long getCompileTime(File f) 
	{
		try
		{
			ZipFile zip = new ZipFile(f);
			return getCompileTime(getZipEntries(zip));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return -1;
	}

}