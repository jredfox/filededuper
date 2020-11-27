package jredfox.filededuper.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.Files;

import jredfox.filededuper.Main;
import jredfox.filededuper.config.csv.CSV;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;
import jredfox.filededuper.util.JarUtil;

public class Commands {
	
	public static final String hashHeader = "#name, md5, sha-1, sha-256, date-modified, compileTime(jar only), boolean modified(jar only), enum consistency(jar only), path";
	public static Command<File> genHashes = new Command<File>("genHashes", "genSpreadsheet")
	{
		@Override
		public File[] getParams(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				return new File[]{this.nextFile("input dir to gen a spreadsheet:")};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}
		
		@Override
		public void run(File... args) 
		{
			File dir = args[0];
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.genExt);
			if(!dir.exists() || files.isEmpty())
			{
				System.out.println("ERR file not found:" + dir);
				return;
			}
			List<String> index = new ArrayList<>(files.size());
			index.add(hashHeader);
			Set<String> hashes = new HashSet<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genHashes(dir, file, hashes, index);
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
	
	public static Command<File> genArchiveHashes = new Command<File>("genArchiveHashes")
	{
		@Override
		public File[] getParams(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input dir to gen a spreadsheet:");
				return new File[]{file};
			}
			return new File[]{ DeDuperUtil.newFile(inputs[0])};
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
			index.add(hashHeader);
			Set<String> hashes = new HashSet<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genHashes(dir, file, hashes, index);
				String hash = DeDuperUtil.getCompareHash(file);
				CSV csv = new CSV(new File(outArchive, DeDuperUtil.getTrueName(file) + "-" + hash + ".csv"));
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
	
	public static Command<File> genDupeHashes = new Command<File>("genDupeHashes")
	{
		@Override
		public String[] getArgs() 
		{
			return new String[]{"File"};
		}

		@Override
		public File[] getParams(String... args)
		{
			if(this.hasScanner(args))
			{
				return new File[]{this.nextFile("input file to gen dupes:")};
			}
			return new File[]{DeDuperUtil.newFile(args[0])};
		}

		@Override
		public void run(File... args)
		{
			File dir = args[0];
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.genDupesExt);
			if(!dir.exists() || files.isEmpty())
			{
				System.out.println("ERR file not found:" + dir);
				return;
			}
			List<String> index = new ArrayList<>(files.size());
			index.add(hashHeader + ", duplicated with");
			Map<String, String> hashes = new HashMap<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genDupeHashes(dir, file, hashes, index);
			}
			File outputFile = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + "-dupes.csv");
			IOUtils.saveFileLines(index, outputFile, true);
		}
	};
	
	public static Command<File> compareHashes = new Command<File>("compareHashes", "compareSpreadSheets")
	{	
		@Override
		public File[] getParams(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File origin = this.nextFile("input origin.csv");
				File compare = this.nextFile("input compare.csv");
				return new File[]{origin, compare};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0]), DeDuperUtil.newFile(inputs[1])};
		}
		
		@Override
		public void run(File... files) 
		{
			CSV origin = new CSV(files[0]);
			CSV compare = new CSV(files[1]);
			CSV output = new CSV(new File(origin.file.getParent(), DeDuperUtil.getTrueName(compare.file) + "-compared.csv"));
			output.add(hashHeader);
			origin.parse();
			compare.parse();
			
			//fetch the hashes from the origin
			int compareIndex = Main.compareHash.ordinal() + 1;
			Set<String> hashes = new HashSet<>(origin.lines.size());
			for(String[] line : origin.lines)
			{
				String hash = line[compareIndex].toLowerCase();
				DeDuperUtil.validateHash(hash);
				hashes.add(hash);
			}
			
			//inject any new entries
			for(String[] line : compare.lines)
			{
				String hash = line[compareIndex].toLowerCase();
				DeDuperUtil.validateHash(hash);
				if(!hashes.contains(hash) && DeDuperUtil.isExt(line[0], Main.compareExt))
				{
					line[1] = DeDuperUtil.caseString(line[1], Main.lowercaseHash);
					line[2] = DeDuperUtil.caseString(line[2], Main.lowercaseHash);
					line[3] = DeDuperUtil.caseString(line[3], Main.lowercaseHash);
					output.lines.add(line);
					hashes.add(hash);
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
			return new String[]{"csv & origin csv"};
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

		@SuppressWarnings("rawtypes")
		@Override
		public void run(Object... args) 
		{
			for(Command c : Command.cmds.values())
			{
				System.out.println(c.name + " " + DeDuperUtil.toString(c.getArgs(), " OR "));
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
			if(this.hasScanner(inputs))
			{
				File toCheck = this.nextFile("input jar to check:");
				File origin = this.nextFile("input jar of origin:");
				return new File[]{toCheck, origin};
			}
			File jar = DeDuperUtil.newFile(inputs[0]);
			File jarOrg = inputs.length == 1 ? jar : DeDuperUtil.newFile(inputs[1]);
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
	public static Command<File> getTimeStamp = new Command<File>("getTimeStamp")
	{
		@Override
		public File[] getParams(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input file to getTimeStamp:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
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
	public static Command<Object> setTimeStamp = new Command<Object>("setTimeStamp")
	{
		@Override
		public Object[] getParams(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input file:");
				String strlong = this.next("input timestamp in ms:");
				long timestamp = DeDuperUtil.parseTimeStamp(strlong);
				return new Object[]{file, timestamp};
			}
			return new Object[]{DeDuperUtil.newFile(inputs[0]), DeDuperUtil.parseTimeStamp(inputs[1])};
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
	public static Command<Object> setTimeStampArchive = new Command<Object>("setTimeStampArchive")
	{
		@Override
		public Object[] getParams(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input archive:");
				String strlong = this.next("input timestamp in ms:");
				long timestamp = DeDuperUtil.parseTimeStamp(strlong);
				return new Object[]{file, timestamp};
			}
			return new Object[]{DeDuperUtil.newFile(inputs[0]), DeDuperUtil.parseTimeStamp(inputs[1])};
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
	
	public static Command<File> printJarInConsistencies = new Command<File>("printJarInConsistencies")
	{
		@Override
		public File[] getParams(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input jar to check:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
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
				System.out.println("jar was consistent:" + ("" + consistent).toUpperCase() + " ---> " + file);
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
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input jar to check:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
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
	
	public static Command<File> getCompileTime = new Command<File>("getCompileTime")
	{
		@Override
		public String[] getArgs()
		{
			return new String[]{"Jar"};
		}

		@Override
		public File[] getParams(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input jar:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(File... args) 
		{
			System.out.println("compileTime:" + JarUtil.getCompileTime(args[0]));
		}
	};
	
	public static Command<Object> currentMs = new Command<Object>("currentMs")
	{
		@Override
		public String[] getArgs() 
		{
			return new String[]{""};
		}

		@Override
		public Object[] getParams(String... cmdArgs) 
		{
			return null;
		}

		@Override
		public void run(Object... args) 
		{
			System.out.println("currentMs:" + System.currentTimeMillis());
		}
	};
	
	/**
	 * get a command based on it's id
	 */
	protected static void load(){}
	
}
