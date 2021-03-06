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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jredfox.filededuper.Main;
import jredfox.filededuper.Main.HashType;
import jredfox.filededuper.PointTimeEntry;
import jredfox.filededuper.archive.ArchiveEntry;
import jredfox.filededuper.archive.Zip;
import jredfox.filededuper.command.ParamList;
import jredfox.filededuper.command.exception.CommandRuntimeException;
import jredfox.filededuper.config.csv.CSV;
import jredfox.filededuper.exception.CompileTimeException;
import jredfox.selfcmd.util.OSUtil;

public class JarUtil {
	
	public static enum Consistencies
	{
		consistentJar(),
		consistentResource(),
		hasConsistentResource(),
		consistentClass(),
		inconsistentClass(),
		inconsistentResource(),
		matchingClasses(),
		none();
	}
	
	/**
	 * is cpu & disk intensive
	 */
	public static boolean isJarModded(boolean consistentJar, File file, List<ZipEntry> entries, boolean signed)
	{
		JarFile jar = null;
		boolean modded = false;
		try
		{
			jar = new JarFile(file, true);
			modded = !checkMetainf(jar, entries, signed) || !getModdedFiles(consistentJar, jar, entries).isEmpty();
		}
		catch(SecurityException e)
		{
			modded = true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		IOUtils.close(jar);
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
	public static boolean dumpJarMod(boolean consistentJar, File file)
	{
		ZipFile zip = null;
		try
		{
			zip = new ZipFile(file);
			List<ArchiveEntry> entries = getModdedFiles(consistentJar, zip);
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
	
	public static List<ArchiveEntry> getModdedFiles(boolean consistentJar, ZipFile zip)
	{
		return getModdedFiles(consistentJar, zip, getZipEntries(zip));
	}
	
	/**
	 * check the jar self integrity with itself
	 */
	public static List<ArchiveEntry> getModdedFiles(boolean consistentJar, ZipFile zip, List<ZipEntry> entries)
	{
		List<ArchiveEntry> entriesOut = new ArrayList<>();
		long compileTime = getCompileTime(getFile(zip), entries, Main.compileTimePoints);
		long minTime = getMinTime(compileTime);
		long maxTime = getMaxTime(compileTime);
		
		for(ZipEntry entry : entries)
		{
			if(isEntryModified(consistentJar, entry, compileTime, minTime, maxTime))
			{
				entriesOut.add(new ArchiveEntry(zip, entry));
			}
		}
		return entriesOut;
	}
	
	public static File getFile(ZipFile zip) 
	{
		return new File(zip.getName());
	}

	public static boolean isEntryModified(boolean consistentJar, ZipEntry entry, long compileTime, long minTime, long maxTime)
	{
		long time = entry.getTime();
		if(consistentJar && time != compileTime)
		{
			return true;
		}
		else if(time > maxTime)
		{
			return true;
		}
		else if(time < minTime && DeDuperUtil.isProgramExt(entry.getName()))
		{
			return true;
		}
		return false;
	}
	
	public static long getMaxTime(long compileTime)
	{
		long maxTime = compileTime + ( (1000L * 60L) * Main.compileTimeOffset);
		return maxTime;
	}

	public static long getMinTime(long compileTime) 
	{
		long minTime = compileTime - ( (1000L * 60L) * Main.compileTimeOffset);
		return minTime;
	}

	/**
	 * compare jar with the original jar file and output any modifications
	 */
	public static List<ArchiveEntry> getModdedFiles(ZipFile zip, ZipFile orgZip)
	{
		List<ZipEntry> entries = getZipEntries(zip);
		List<ArchiveEntry> entriesOut = new ArrayList<>();
		for(ZipEntry entry : entries)
		{
			ZipEntry orgEntry = orgZip.getEntry(entry.getName());
			if(orgEntry == null)
			{
				entriesOut.add(new ArchiveEntry(zip, entry, new ZipEntry((Main.archiveDir ? "added/" : "mod/") + entry.getName())));
				continue;
			}
			String hash = DeDuperUtil.getCompareHash(zip, entry);
			String orgHash = DeDuperUtil.getCompareHash(orgZip, orgEntry);
			if(!hash.equals(orgHash))
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
	
	public static long getCompileTime(File file, boolean points)
	{
		ZipFile zip = null;
		try
		{
			return getCompileTime(file, JarUtil.getZipEntries(new ZipFile(file)), points);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(zip);
		}
		return -1;
	}
	
	public static long getCompileTimeSafley(File zipFile, List<ZipEntry> entries, boolean points)
	{
		try
		{
			return getCompileTime(zipFile, entries, points);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return -1;
	}
	
	public static long getCompileTime(File zipFile, List<ZipEntry> entries, boolean points) throws CompileTimeException
	{
		return points ? getCompileTimePoints(zipFile, entries, Main.programDirs, Main.libDirs) : getCompileTimeLeast(zipFile, entries, Main.programDirs, Main.libDirs);
	}
	
	/**
	 * gets the most likely compiledTime based on lowest date modified of the class file
	 */
	public static long getCompileTimeLeast(File zipFile, List<ZipEntry> entries, String[] programDirs, String[] libDirs)
	{
		if(programDirs != libDirs && isLibrary(entries))
			return getCompileTimeLeast(zipFile, entries, libDirs, libDirs);
		long ms = -1;
		for(ZipEntry e : entries)
		{
			String name = e.getName();
			if(!name.endsWith(".class") || !isDir(e, programDirs))
				continue;
			long time = e.getTime();
			if(time < ms || ms == -1)
			{
				ms = time;
			}
		}
		if(ms == -1)
			throw new CompileTimeException("JarUtil#getCompileTimeLeast() couldn't find program dir add one to the config:" + zipFile.getAbsolutePath());
		return ms;
	}
	
	/**
	 * @returns if the jar has a library directory and contains no programDirs
	 */
	public static boolean isLibrary(List<ZipEntry> entries) 
	{
		boolean libDirs = false;
		for(ZipEntry e : entries)
		{
			if(isDir(e, Main.programDirs))
			{
				return false;
			}
			if(isDir(e, Main.libDirs))
			{
				libDirs = true;
			}
		}
		return libDirs;
	}

	/**
	 * @return true if it is the right program directory
	 */
	public static boolean isDir(ZipEntry e, String[] dirs)
	{
		String name = e.getName();
		if(!name.contains("/") && !e.isDirectory())
			name = PointTimeEntry.defaultDir;
		for(String dir : dirs)
		{
			if(name.startsWith(dir))
				return true;
		}
		return false;
	}

	/**
	 * gets compile time based on program dir points
	 */
	public static long getCompileTimePoints(File zipFile, List<ZipEntry> entries, String[] programDirs, String[] libDirs)
	{
		if(programDirs != libDirs && isLibrary(entries))
			return getCompileTimePoints(zipFile, entries, libDirs, libDirs);
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
				if(point.add(entry, time))
				{
					added = true;
					break;
				}
			}
			if(!added)
			{
				PointTimeEntry point = new PointTimeEntry(programDirs, entry, time);
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
						return ((Integer)p2.getPoints()).compareTo(p.getPoints());
					}
				}
				);
		if(points.isEmpty())
			throw new CompileTimeException("JarUtil#getCompileTimePoints() couldn't find program dir add one to the config:" + zipFile.getAbsolutePath());
		PointTimeEntry point = points.get(0);
		long ms = point.getPointEntry().getKey();
		return ms;
	}
	
	public static boolean checkMetainf(JarFile jar, List<ZipEntry> entries, boolean signed) throws IOException 
	{
		if(signed)
		{
			ZipEntry dsa = getDSA(jar);
			ZipEntry rsa = getRSA(jar);
			if(!checkSignature(jar, dsa) && !checkSignature(jar, rsa))
			{
				return false;
			}
			
			ZipEntry sf = getSF(jar);
			if(sf == null || !checkManifest(entries, new Manifest(jar.getInputStream(sf)), signed))
			{
				return false;
			}
		}
		Manifest mani = jar.getManifest();
		return checkManifest(entries, mani, signed);
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

	public static boolean checkManifest(List<ZipEntry> entries, Manifest mf, boolean signed)
	{
		if(mf == null || mf.getMainAttributes().isEmpty())
		{
			return false;
		}
		if(signed)
		{
			if(mf.getEntries().isEmpty())
				return false;
			List<String> names = new ArrayList<>(mf.getEntries().entrySet().size());
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
			List<String> actualNames = new ArrayList<>();
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
		return DeDuperUtil.getSafley(getEntriesFromDir(jar, "META-INF/", "sf"), 0);
	}
	
	private static ZipEntry getRSA(JarFile jar) {
		return DeDuperUtil.getSafley(getEntriesFromDir(jar, "META-INF/", "rsa"), 0);
	}

	private static ZipEntry getDSA(JarFile jar) {
		return DeDuperUtil.getSafley(getEntriesFromDir(jar, "META-INF/", "dsa"), 0);
	}
	
	public static List<ZipEntry> getEntriesFromDir(ZipFile file, String path, String ext)
	{
		ext = ext.toLowerCase();
		List<ZipEntry> list = new ArrayList<>(2);
		List<ZipEntry> entries = getZipEntries(file);
		for(ZipEntry e : entries)
		{
			if(e.isDirectory())
				continue;
			String name = e.getName();
			if(name.startsWith(path) && DeDuperUtil.isFileExt(name, ext))
			{
				list.add(e);
			}
		}
		return list;
	}
	
	public static List<ZipEntry> getZipEntries(ZipFile zip) 
	{
		if(zip == null)
			return null;
		List<ZipEntry> entryList = new ArrayList<>(150);
		Enumeration<?> entries = zip.entries();
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
		List<ArchiveEntry> archs = new ArrayList<>(entries.size());
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

	public static void addArchiveEntries(ParamList<?> params, File f, CSV csv)
	{
		try
		{
			boolean plugin = !params.hasFlag("skipPluginGen");
			boolean consistentJar = params.hasFlag("consistentJar");
			ZipFile zip = new ZipFile(f);
			List<ZipEntry> entries = JarUtil.getZipEntries(zip);
			Set<String> hashes = new HashSet<>(entries.size());
			csv.add("#name, md5, sha-1, sha256, size, date-modified, compileTime, boolean modified, enum consistency, path");
			for(ZipEntry entry : entries)
			{
				if(entry.isDirectory())
					continue;
				String name = new File(entry.getName()).getName();
				String hash = DeDuperUtil.getCompareHash(zip, entry);
				if(hashes.contains(hash))
				{
					String[] line = csv.getLine(hash, Main.compareHash.ordinal() + 1);//get the line based on the hash colum
					if(plugin) 
					{
						System.out.println("skipping:" + entry.getName());
						skipLine(f, entries, entry, csv, consistentJar, line);
					}
					continue;
				}
				String md5 = Main.compareHash == HashType.MD5 ? hash : DeDuperUtil.getMD5(zip, entry);
				String sha1 = Main.compareHash == HashType.SHA1 ? hash : DeDuperUtil.getSHA1(zip, entry);
				String sha256 = Main.compareHash == HashType.SHA256 ? hash : DeDuperUtil.getSHA256(zip, entry);
				long size = entry.getSize();
				long time = entry.getTime();
				String pluginData = JarUtil.getPlugin(params, zip, entries, entry);
				String path = entry.getName();
				csv.add(name + "," + md5 + "," + sha1 + "," + sha256 + "," + size + "," + time + (pluginData.isEmpty() ? "" : "," + pluginData) + "," + path);
				hashes.add(md5);
			}
			zip.close();
		}
		catch(CommandRuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getPlugin(ParamList<?> params, ZipFile zip, List<ZipEntry> entries, ZipEntry entry) 
	{
		try
		{
			String ext = DeDuperUtil.getExtension(zip.getName());
			return params.hasFlag("skipPluginGen") || !DeDuperUtil.isExtEqual(ext, DeDuperUtil.pluginExts) ? "" : genJarData(params, zip, entries, entry);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "exception_" + e.getClass().getName();
		}
	}

	private static String genJarData(ParamList<?> params, ZipFile zip, List<ZipEntry> entries, ZipEntry entry) 
	{
		boolean consistentJar = params.hasFlag("consistentJar");
		long compileTime = JarUtil.getCompileTimeSafley(JarUtil.getFile(zip), entries, Main.compileTimePoints);
		long minTime = JarUtil.getMinTime(compileTime);
		long maxTime = JarUtil.getMaxTime(compileTime);
		boolean modified = isEntryModified(consistentJar, entry, compileTime, minTime, maxTime);
		Consistencies consistencies = getConsistency(entry, compileTime, entry.getTime());
		return compileTime + "," + modified + "," + consistencies;
	}

	private static void skipLine(File zipFile, List<ZipEntry> entries, ZipEntry entry, CSV csv, boolean consistentJar, String[] line) 
	{
		if(DeDuperUtil.getExtension(zipFile).equals("jar"))
		{
			long compileTime = JarUtil.getCompileTimeSafley(zipFile, entries, Main.compileTimePoints);
			long minTime = JarUtil.getMinTime(compileTime);
			long maxTime = JarUtil.getMaxTime(compileTime);
			if(isEntryModified(consistentJar, entry, compileTime, minTime, maxTime))
			{
				String tst = line[Main.jarModifiedIndex];
				if( !(tst.equalsIgnoreCase("true") || tst.equalsIgnoreCase("false")))
					throw new CommandRuntimeException("cannot find the jar modification boolean index!");
				line[Main.jarModifiedIndex] = "true";
			}
		}
	}

	/**
	 * gets the consistencies of an individual ZipEntry
	 */
	public static Consistencies getConsistency(ZipEntry entry, long compileTime, long time)
	{
		boolean consistent = compileTime == time;
		boolean programFile = DeDuperUtil.isProgramExt(entry.getName());
		return consistent ? (programFile ? Consistencies.consistentClass : Consistencies.consistentResource) : (programFile ? Consistencies.inconsistentClass : Consistencies.inconsistentResource);
	}

	/**
	 * gets the consistency of the entire jar file.
	 * @return {@link Consistencies#consistentJar} if all files are consistent.
	 *  {@link Consistencies#consistentResource} if all resources match the compile time but, the classes are inconsistent
	 * {@link Consistencies#hasConsistentResource} if one or more resources match the compile time
	 * {@link Consistencies#matchingClasses} if all the program files match the timestamp but, have no matching resources
	 * {@link Consistencies#none} if the jar's resources are inconsistent with compile time not all classes are matching and no resources are matching.
	 * this isn't a boolean for is modded just an enum for how the compiler/obfuscator/jar signer works.
	 */
	public static Consistencies getConsistentcy(File f, List<ZipEntry> entries, long compileTime) 
	{
		try
		{
			Set<Consistencies> cs = new HashSet<>(4);
			for(ZipEntry e : entries)
			{
				if(e.isDirectory())
					continue;
				long time = e.getTime();
				Consistencies c = getConsistency(e, compileTime, time);
				cs.add(c);
			}
			return !cs.contains(Consistencies.inconsistentClass) && !cs.contains(Consistencies.inconsistentResource) ? 
					Consistencies.consistentJar : 
						(cs.contains(Consistencies.consistentResource) ? 
								(cs.contains(Consistencies.inconsistentResource) ? 
										Consistencies.hasConsistentResource : Consistencies.consistentResource) : 
											cs.contains(Consistencies.inconsistentClass) ? 
													Consistencies.none : Consistencies.matchingClasses);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return Consistencies.none;
	}

	public static Zip getZipFile(File file)
	{
		try
		{
			return new Zip(file);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * avoids unzipping app archives such as jars or jar mod. populates a list of files that are program files
	 */
	public static void deepNonAppUnzip(List<File> files, File zipFile) throws IOException
	{
		deepNonAppUnzip(files, zipFile, new File(zipFile.getParent() + "/" + DeDuperUtil.getTrueName(zipFile)));
	}
	
	/**
	 * avoids unzipping app archives such as jars or jar mod. populates a list of files that are program files
	 */
	public static void deepNonAppUnzip(List<File> apps, File zipFile, File outDir) throws ZipException, IOException
	{
		ZipFile zip = new ZipFile(zipFile);
		List<ZipEntry> entries = JarUtil.getZipEntries(zip);
		if(isApp(entries))
		{
			apps.add(zipFile);
			zip.close();
			return;
		}
		for(ZipEntry entry : entries)
		{
			File dumped = new File(outDir, entry.getName());
			String ext = DeDuperUtil.getExtension(dumped);
			unzip(zip, entry, dumped);
			if(DeDuperUtil.isExtEqual(ext, Main.archiveExt))
			{
				deepNonAppUnzip(apps, dumped, new File(dumped.getParent(), DeDuperUtil.getTrueName(dumped)));
			}
		}
		zip.close();
	}
	
	/**
	 * returns if the specified zip file contains app/program files
	 */
	public static boolean isApp(List<ZipEntry> entries)
	{
		for(ZipEntry entry : entries)
		{
			String name = entry.getName();
			if(DeDuperUtil.isProgramExt(name))
			{
				return true;
			}
		}
		return false;
	}

	public static void deepUnzip(File zipFile, boolean hasExt) throws IOException
	{
		deepUnzip(zipFile, new File(zipFile.getParent() + "/" + DeDuperUtil.getTrueName(zipFile)), hasExt);
	}
	
	public static void deepUnzip(File zipFile, File outDir, boolean hasExt) throws IOException
	{
		//fix the output directory
		String zipExt = DeDuperUtil.getExtension(zipFile);
		if(hasExt && !outDir.getName().endsWith("-" + zipExt))
			outDir = new File(outDir.getParent(), outDir.getName() + "-" + zipExt);
		
		//start deep unzipping
		ZipFile zip = JarUtil.getZipFile(zipFile);
		if(zip == null)
			return;//return if the ZipFile is encrypted
		List<ZipEntry> entries = JarUtil.getZipEntries(zip);
		for(ZipEntry entry : entries)
		{
			File dumped = new File(outDir, entry.getName());
			String ext = DeDuperUtil.getExtension(dumped);
			unzip(zip, entry, dumped);
			if(DeDuperUtil.isExtEqual(ext, Main.archiveExt))
			{
				deepUnzip(dumped, new File(dumped.getParent(), DeDuperUtil.getTrueName(dumped) + (hasExt ? "-" + ext : "")), hasExt);
			}
		}
		zip.close();
	}

	public static void unzip(ZipFile zip, ZipEntry entry, File file) throws IOException 
	{
		file = OSUtil.toWinFile(file);
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		ZipEntry newEntry = new ZipEntry(entry.getName());
		newEntry.setTime(entry.getTime());
		IOUtils.copy(zip.getInputStream(newEntry), out);
		out.close();
		file.setLastModified(entry.getTime());
	}

}
