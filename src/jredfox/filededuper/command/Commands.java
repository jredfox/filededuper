package jredfox.filededuper.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.Files;

import jredfox.filededuper.Main;
import jredfox.filededuper.config.csv.CSV;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.filededuper.util.JarUtil;

public class Commands {
	
	public static Map<String, Command> cmds = new TreeMap<>();
	
	/**
	 * get a command based on it's id
	 */
	public static Command getCommand(String id)
	{
		return cmds.get(id);
	}
	
	public static Command<File> genMD5s = new Command<File>("genMD5s")
	{
		@Override
		public File[] getParams(String... inputs) 
		{
			File dir = null;
			if(inputs.length < 2)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input directory to generate a spreadsheet:");
				dir = new File(scanner.nextLine());
				if(!dir.exists())
				{
					throw new CMDMaulformedException("directory or file doesn't exist:" + dir);
				}
			}
			else
			{
				dir = new File(inputs[1]);
			}
			return new File[]{ dir };
		}
		
		@Override
		public void run(File... args) 
		{
			File dir = args[0];
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.genExt);
			if(!dir.exists() || files.isEmpty())
				return;
			List<String> index = new ArrayList<>(files.size());
			index.add("#name, md5, sha-256, date-modified, compileTime(jar only), boolean modified(jar only), enum consistency(jar only), path");
			Set<String> md5s = new HashSet<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genMD5s(dir, file, md5s, index);
			}
			File outputFile = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + ".csv");
			IOUtils.saveFileLines(index, outputFile, true);
		}

		@Override
		public String[] getArgs()
		{
			return new String[]{"Dir/File"};
		}
	};
	
	public static Command<File> genArchiveMD5s = new Command<File>("genArchiveMD5s")
	{
		@Override
		public File[] getParams(String... inputs) 
		{
			File dir = null;
			if(inputs.length < 2)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input directory to generate a spreadsheet:");
				dir = new File(scanner.nextLine());
				if(!dir.exists())
				{
					throw new CMDMaulformedException("directory or file doesn't exist:" + dir);
				}
			}
			else
			{
				dir = new File(inputs[1]);
			}
			return new File[]{ dir };
		}

		@Override
		public void run(File... args)
		{
			File dir = args[0];
			List<File> files = DeDuperUtil.getDirFiles(dir, "jar", "zip");
			if(!dir.exists() || files.isEmpty())
				return;
			File outDir = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + "-output");
			File outArchive = new File(outDir, "archives");
			outArchive.mkdirs();
			List<String> index = new ArrayList<>(files.size());
			index.add("#name, md5, sha-256, date-modified, compileTime(jar only), boolean modified(jar only), enum consistency, path");
			Set<String> md5s = new HashSet<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genMD5s(dir, file, md5s, index);
				String md5 = DeDuperUtil.getMD5(file);
				CSV csv = new CSV(new File(outArchive, DeDuperUtil.getTrueName(file) + "-" + md5 + ".csv"));
				boolean isJar = DeDuperUtil.getExtension(file).equals("jar");
				if(isJar)
				{
					JarUtil.addJarEntries(file, csv);
				}
				else
				{
					JarUtil.addZipEntries(file, csv);
				}
				csv.save();
			}
			File outputIndex = new File(outDir, "index-" + DeDuperUtil.getTrueName(dir) + ".csv");
			IOUtils.saveFileLines(index, outputIndex, true);
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"Dir/File"};
		}
	};
	
	public static Command<File> compareMD5s = new Command<File>("compareMD5s")
	{	
		@Override
		public File[] getParams(String... inputs)
		{
			if(inputs.length < 2)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input origin.csv");
				File origin = new File(scanner.nextLine());
				System.out.println("input compare.csv");
				File compare = new File(scanner.nextLine());
				return new File[]{origin, compare};
			}
			return new File[]{new File(inputs[1]), new File(inputs[2])};
		}
		
		@Override
		public void run(File... files) 
		{
			CSV origin = new CSV(files[0]);
			CSV compare = new CSV(files[1]);
			CSV output = new CSV(new File(origin.file.getParent(), DeDuperUtil.getTrueName(origin.file) + "-compared.csv"));
			output.add("#name, md5, sha-256, date-modified, boolean modified(jar only), path");
			origin.parse();
			compare.parse();
			
			//fetch the md5s from the origin
			Set<String> md5s = new HashSet(origin.lines.size());
			for(String[] line : origin.lines)
			{
				md5s.add(line[1].toLowerCase());
			}
			
			//inject any new entries
			for(String[] line : compare.lines)
			{
				String md5 = line[1].toLowerCase();
				if(!md5s.contains(md5) && (Main.compareExt[0].equals("*") || DeDuperUtil.endsWith(line[0], Main.compareExt)) )
				{
					line[1] = DeDuperUtil.caseString(line[1], Main.lowercaseHash);
					line[2] = DeDuperUtil.caseString(line[2], Main.lowercaseHash);
					output.lines.add(line);
					md5s.add(md5);
				}
			}
			
			output.file.delete();
			if(output.lines.size() > 1)
			{
				output.save();
			}
			else
			{
				System.out.println("NO NEW FILES FOUND");
			}
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"csv", "origin csv"};
		}
	};
	
	public static Command<File> deDupe = new Command<File>("deDupe")
	{
		@Override
		public File[] getParams(String... inputs) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void run(File... agrs) {
			// TODO Auto-generated method stub
		}

		@Override
		public String[] getArgs() {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	public static Command<Object> help = new Command<Object>("help")
	{
		@Override
		public Object[] getParams(String... inputs) 
		{
			return null;
		}

		@Override
		public void run(Object... args) 
		{
			for(Command c : Commands.cmds.values())
			{
				System.out.println(c.id + " " + DeDuperUtil.toString(c.getArgs(), " || "));
			}
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{""};
		}
	};

	public static Command<File> checkJar = new Command<File>("checkJar")
	{
		@Override
		public File[] getParams(String... inputs)
		{
			if(inputs.length == 1)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input jar to check:");
				File toCheck = new File(scanner.nextLine());
				System.out.println("input jar of origin:");
				File origin = new File(scanner.nextLine());
				return new File[]{toCheck, origin};
			}
			File jar = new File(inputs[1]);
			File jarOrg = inputs.length == 3 ? new File(inputs[2]) : jar;
			return new File[]{jar, jarOrg};
		}

		@Override
		public void run(File... args)
		{
			try
			{
				if(args[0].equals(args[1]))
				{
					if(JarUtil.dumpJarMod(args[0]))
						System.out.println("Dumped jarMod");
				}
				else
				{
					if(JarUtil.dumpJarMod(args[0], args[1]))
						System.out.println("Dumped jarMod");
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"Jar", "Archive-File & Archive-File"};
		}
	};
	
	/**
	 * sets the time stamp to a specified file
	 */
	public static Command<Object> setTimeStamp = new Command<Object>("setTimeStamp")
	{
		@Override
		public Object[] getParams(String... inputs) 
		{
			if(inputs.length == 1)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input file:");
				File file = new File(scanner.nextLine());
				System.out.println("input timestamp in ms:");
				long timestamp = Long.parseLong(scanner.nextLine());
				return new Object[]{file, timestamp};
			}
			return new Object[]{new File(inputs[1]), Long.parseLong(inputs[2])};
		}

		@Override
		public void run(Object... args) 
		{
			File file = (File) args[0];
			long timestamp = (long) args[1];
			file.setLastModified(timestamp);
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"File"};
		}
	};
	
	/**
	 * sets the time stamp to a specified file
	 */
	public static Command<File> getTimeStamp = new Command<File>("getTimeStamp")
	{
		@Override
		public File[] getParams(String... inputs) 
		{
			if(inputs.length == 1)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input file to getTimeStamp:");
				File file = new File(scanner.nextLine());
				return new File[]{file};
			}
			return new File[]{new File(inputs[1])};
		}

		@Override
		public void run(File... args) 
		{
			File file = (File) args[0];
			System.out.println(file.exists() ? ("" + file.lastModified()) : ("INVALID FILE" + file));
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"File"};
		}
	};
	
	/**
	 * sets the time stamp to a specified file
	 */
	public static Command<Object> setTimeStampArchive = new Command<Object>("setTimeStampArchive")
	{
		@Override
		public Object[] getParams(String... inputs) 
		{
			if(inputs.length == 1)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input file");
				File file = new File(scanner.nextLine());
				System.out.println("input timestamp in ms:");
				long timestamp = Long.parseLong(scanner.nextLine());
				return new Object[]{file, timestamp};
			}
			return new Object[]{new File(inputs[1]), Long.parseLong(inputs[2])};
		}

		@Override
		public void run(Object... args) 
		{
			try
			{
				File file = (File) args[0];
				ZipFile zip = new ZipFile(file);
				long timestamp = (long) (Long) args[1];
				List<ZipEntry> entries = JarUtil.getZipEntries(zip);
				for(ZipEntry e : entries)
				{
					e.setTime(timestamp);
				}
				File output = new File(file.getParent(), DeDuperUtil.getTrueName(file) + "-tmp.zip");
				JarUtil.saveZip(JarUtil.getArchiveEntries(zip, entries), output);
				zip.close();
				Files.move(output, file);
				file.setLastModified(timestamp);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public String[] getArgs()
		{
			return new String[]{"Archive-File"};
		}
	};
	
	public static Command<File> isJarConsistent = new Command<File>("isJarConsistent")
	{
		@Override
		public File[] getParams(String... inputs)
		{
			if(inputs.length == 1)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input jar to check:");
				File file = new File(scanner.nextLine());
				return new File[]{file};
			}
			return new File[]{new File(inputs[1])};
		}

		@Override
		public void run(File... args) 
		{
			try
			{
				File file = args[0];
				ZipFile zip = new ZipFile(file);
				List<ZipEntry> entries = JarUtil.getZipEntries(zip);
				long timestamp = JarUtil.getCompileTime(entries);
				boolean consistent = true;
				for(ZipEntry e : entries)
				{
					if(e.isDirectory())
						continue;
					long time = e.getTime();
					if(timestamp != time)
					{
						consistent = false;
						System.out.println("inconsistent:\t" + e.getName() + ", time:" + time +  " with:" + timestamp);
					}
				}
				System.out.println(consistent + " ---> " + file);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"Jar"};
		}	
	};
	
	public static Command<File> printJarConsistencies = new Command<File>("printJarConsistencies")
	{
		@Override
		public File[] getParams(String... inputs)
		{
			if(inputs.length == 1)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input jar to check:");
				File file = new File(scanner.nextLine());
				return new File[]{file};
			}
			return new File[]{new File(inputs[1])};
		}

		@Override
		public void run(File... args) 
		{
			try
			{
				File file = args[0];
				ZipFile zip = new ZipFile(file);
				List<ZipEntry> entries = JarUtil.getZipEntries(zip);
				long timestamp = JarUtil.getCompileTime(entries);
				boolean consistent = false;
				for(ZipEntry e : entries)
				{
					if(e.isDirectory())
						continue;
					long time = e.getTime();
					if(timestamp == time && !DeDuperUtil.isProgramExt(e.getName()))
					{
						consistent = true;
						System.out.println("consistent:\t" + e.getName() + ", time:" + time);
					}
				}
				if(consistent)
					System.out.println("file had consistentcies: " + file);
				else
					System.out.println("file had NO consistentcies: " + file);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public String[] getArgs() 
		{
			return new String[]{"Jar"};
		}
	};
	
}
