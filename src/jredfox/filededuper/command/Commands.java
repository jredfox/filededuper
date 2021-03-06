package jredfox.filededuper.command;

import java.io.File;
import java.io.IOException;
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
import jredfox.filededuper.util.list.DummyList;
import jredfox.selfcmd.SelfCommandPrompt;
import jredfox.selfcmd.util.OSUtil;

public class Commands {
	
	public static final String hashHeader = "#name, md5, sha-1, sha-256, size, date-modified, compileTime(jar only), boolean modified(jar only), enum consistency(jar only), path";
	
	public static Command<File> genHashes = new Command<File>(new Object[]{new CommandOption(new String[]{"--unSigned", "--consistentJar"}, "--skipPluginGen"), "--unSigned", "--consistentJar"}, "genHashes", "genSpreadsheet")
	{
		@Override
		public File[] parse(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				return new File[]{this.nextFile("input dir to gen a spreadsheet:")};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}
		
		@Override
		public void run(ParamList<File> params) 
		{
			File dir = params.get(0);
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.genExt);
			if(!dir.exists() || files.isEmpty())
			{
				System.err.println("Error file not found:" + dir);
				return;
			}
			List<String> index = new ArrayList<>(files.size());
			index.add(hashHeader);
			Set<String> hashes = new HashSet<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genHashes(params, dir, file, hashes, index);
			}
			File outputFile = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + ".csv");
			IOUtils.saveFileLines(index, outputFile, true);
		}

		@Override
		public String[] displayArgs()
		{
			return new String[]{"Dir/File"};
		}
	};
	
	public static Command<File> genArchiveHashes = new Command<File>(new Object[]{ new CommandOption(new String[]{"--unSigned", "--consistentJar"}, "--skipPluginGen"), "--unSigned", "--consistentJar"}, "genArchiveHashes", "genArchiveSpreadsheet")
	{
		@Override
		public File[] parse(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input dir to gen a spreadsheet:");
				return new File[]{file};
			}
			return new File[]{ DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params)
		{
			File dir = params.get(0);
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.archiveExt);
			if(!dir.exists() || files.isEmpty())
			{
				System.err.println("Error file not found:" + dir);
				return;
			}
			File outDir = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + "-output");
			File outArchive = new File(outDir, "archives");
			outArchive.mkdirs();
			List<String> index = new ArrayList<>(files.size());
			index.add(hashHeader);
			Set<String> hashes = new HashSet<>(files.size());
			for(File file : files)
			{
				String hash = DeDuperUtil.getCompareHash(file);
				if(hashes.contains(hash))
				{
					continue;
				}
				DeDuperUtil.genHashes(params, dir, file, hashes, index);
				CSV csv = new CSV(new File(outArchive, file.getName() + "-" + hash + ".csv"));
				JarUtil.addArchiveEntries(params, file, csv);
				if(csv.lines.size() > 1)
					csv.save();
				System.out.println("finished archive:" + file);
			}
			File outputIndex = new File(outDir, "index-" + DeDuperUtil.getTrueName(dir) + ".csv");
			IOUtils.saveFileLines(index, outputIndex, true);
		}

		@Override
		public String[] displayArgs() 
		{
			return new String[]{"Archive-File"};
		}
	};
	
	public static Command<File> genDupeHashes = new Command<File>(new Object[]{ new CommandOption(new String[]{"--unSigned", "--consistentJar"}, "--skipPluginGen"), "--unSigned", "--consistentJar"}, "genDupeHashes")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"File"};
		}

		@Override
		public File[] parse(String... args)
		{
			if(this.hasScanner(args))
			{
				return new File[]{this.nextFile("input file to gen dupes:")};
			}
			return new File[]{DeDuperUtil.newFile(args[0])};
		}

		@Override
		public void run(ParamList<File> params)
		{
			File dir = params.get(0);
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.genDupesExt);
			if(!dir.exists() || files.isEmpty())
			{
				System.err.println("Error file not found:" + dir);
				return;
			}
			List<String> index = new ArrayList<>(files.size());
			index.add(hashHeader + ", duplicated with");
			Map<String, File> hashes = new HashMap<>(files.size());
			for(File file : files)
			{
				DeDuperUtil.genDupeHashes(params, dir, file, hashes, index);
			}
			if(index.size() > 1)
			{
				File outputFile = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + "-dupes.csv");
				IOUtils.saveFileLines(index, outputFile, true);
			}
			else
			{
				System.out.println("NO DUPE FILES FOUND");
			}
		}
	};
	
	public static Command<File> compareHashes = new Command<File>("compareHashes", "compareSpreadSheets")
	{	
		@Override
		public File[] parse(String... inputs)
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
		public void run(ParamList<File> params) 
		{
			CSV origin = new CSV(params.get(0));
			CSV compare = new CSV(params.get(1));
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
				if(!hashes.contains(hash) && DeDuperUtil.isFileExt(line[0], Main.compareExt))
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
		public String[] displayArgs() 
		{
			return new String[]{"csv & origin csv"};
		}
	};
	
	public static Command<File> deDupe = new Command<File>(new Object[]{"--flat", new CommandOption(new String[]{"--appUnzip"}, "--skipUnzip"), "--appUnzip", "--genPluginData"}, "deDupe")
	{
		@Override
		public String[] displayArgs()
		{
			return new String[]{"Dir"};
		}
		
		@Override
		public File[] parse(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				return new File[]{this.nextFile("input directory to de-dupe:")};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			File dir = params.get(0);
			File tmp = new File(OSUtil.getAppData(), Main.appId + "/tmp");
			List<File> files;
			
			//deepunzip the files to the appdata so it doesn't screw with your working directory
			if(!params.hasFlag("skipUnzip"))
			{
				List<File> archives = DeDuperUtil.getDirFiles(dir, Main.archiveExt);
				List<File> apps = new ArrayList<>();
				boolean nonAppZip = !params.hasFlag("appUnzip");
				for(File archive : archives)
				{
					try 
					{
						File outZip = new File(tmp, DeDuperUtil.getTrueName(archive));
						if(nonAppZip)
							JarUtil.deepNonAppUnzip(apps, archive, outZip);
						else
							JarUtil.deepUnzip(archive, outZip, true);
					}
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
				
				files = DeDuperUtil.getDirFiles(dir, Main.archiveExt, true);
				files.addAll(apps);
				files.addAll(DeDuperUtil.getDirFiles(tmp, Main.archiveExt, true));//populate unzipped tmp files
			}
			else
			{
				files = DeDuperUtil.getDirFiles(dir);
			}

			File output = new File(dir.getParent(), DeDuperUtil.getTrueName(dir) + "-output");
			Set<String> hashes = DeDuperUtil.getHashes(output);
			boolean flat = params.hasFlag("flat");
			for(File file : files)
			{
				String hash = DeDuperUtil.getCompareHash(file);
				if(!hashes.add(hash))
				{
					System.out.println("skipping:" + hash + ", " + DeDuperUtil.fixPath(tmp, dir, file));
					continue;
				}
				try
				{
					if(flat)
					{
						String[] allHashes = DeDuperUtil.getAllHashes(hash, file);
						String name = DeDuperUtil.getTrueName(file);
						for(String checkHash : allHashes)
						{
							checkHash = "-" + checkHash;
							if(name.contains(checkHash))
								name = name.replaceAll(checkHash, "");
						}
						DeDuperUtil.copy(file, new File(output, name + "-" + hash + DeDuperUtil.getExtensionFull(file)));
					}
					else
					{
						DeDuperUtil.copy(file, new File(output, DeDuperUtil.fixPath(tmp, dir, file)));
					}
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
//			IOUtils.deleteDirectory(tmp);
		}
	};
	
	public static Command<Object> help = new Command<Object>("help")
	{
		@Override
		public Object[] parse(String... inputs) 
		{
			return null;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void run(ParamList<Object> params) 
		{
			for(Command c : Command.cmds.values())
			{
				String names = DeDuperUtil.toString(c.names, ", ");
				System.out.println(names + ", ParamsList:(" + DeDuperUtil.toString(c.displayArgs(), ", ") + ")" + (c.options.isEmpty() ? "" : ", OptionalParams:(" + Command.getOArgsString(c.options, ", ") + ")") );
			}
		}

		@Override
		public String[] displayArgs() 
		{
			return new String[]{""};
		}
	};

	public static Command<File> checkJar = new Command<File>(new String[]{"--consistentJar"}, "checkJar")
	{
		@Override
		public File[] parse(String... inputs)
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
		public void run(ParamList<File> params)
		{
			File jar = params.get(0);
			File orgJar = params.get(1);
			if(jar.equals(orgJar))
			{
				if(JarUtil.dumpJarMod(params.hasFlag("consistentJar"), jar))
					System.out.println("Dumped jarMod");
			}
			else
			{
				if(JarUtil.dumpJarMod(jar, orgJar))
					System.out.println("Dumped jarMod");
			}
		}

		@Override
		public String[] displayArgs() 
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
		public File[] parse(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input file to getTimeStamp:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			File file = params.get(0);
			System.out.println(file.exists() ? ("" + file.lastModified()) : ("INVALID FILE" + file));
		}

		@Override
		public String[] displayArgs() 
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
		public Object[] parse(String... inputs) 
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
		public void run(ParamList<Object> params) 
		{
			File file = params.get(0);
			long timestamp = params.get(1);
			file.setLastModified(timestamp);
		}

		@Override
		public String[] displayArgs() 
		{
			return new String[]{"File & Long"};
		}
	};
	
	/**
	 * sets the time stamp to a specified file
	 */
	public static Command<Object> setTimeStampArchive = new Command<Object>("setTimeStampArchive")
	{
		@Override
		public Object[] parse(String... inputs) 
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
		public void run(ParamList<Object> params) 
		{
			try
			{
				File file = params.get(0);
				ZipFile zip = new ZipFile(file);
				long timestamp = params.get(1);
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
		public String[] displayArgs()
		{
			return new String[]{"Archive-File & Long"};
		}
	};
	
	public static Command<File> printJarInConsistencies = new Command<File>("printJarInConsistencies")
	{
		@Override
		public File[] parse(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input jar to check:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			try
			{
				File file = params.get(0);
				ZipFile zip = new ZipFile(file);
				List<ZipEntry> entries = JarUtil.getZipEntries(zip);
				long timestamp = JarUtil.getCompileTime(file, entries, Main.compileTimePoints);
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
		public String[] displayArgs() 
		{
			return new String[]{"Jar"};
		}
	};
	
	public static Command<File> printJarConsistencies = new Command<File>("printJarConsistencies")
	{
		@Override
		public File[] parse(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input jar to check:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			try
			{
				File file = params.get(0);
				ZipFile zip = new ZipFile(file);
				List<ZipEntry> entries = JarUtil.getZipEntries(zip);
				long timestamp = JarUtil.getCompileTime(file, entries, Main.compileTimePoints);
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
		public String[] displayArgs() 
		{
			return new String[]{"Jar"};
		}
	};
	
	public static Command<File> getCompileTime = new Command<File>(new CommandOption[]{new CommandOption(new String[]{"--points"}, "--least"), new CommandOption("--points")}, "getCompileTime")
	{
		@Override
		public String[] displayArgs()
		{
			return new String[]{"Jar"};
		}

		@Override
		public File[] parse(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				File file = this.nextFile("input jar:");
				return new File[]{file};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			File file =  params.get(0);
			boolean points = Main.compileTimePoints;
			if(params.hasFlag("points"))
				points = true;
			else if(params.hasFlag("least"))
				points = false;
			System.out.println("compileTime:" + JarUtil.getCompileTime(file, points));
		}
	};
	
	public static Command<Object> currentMs = new Command<Object>("currentMs")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{""};
		}

		@Override
		public Object[] parse(String... cmdArgs) 
		{
			return null;
		}

		@Override
		public void run(ParamList<Object> params) 
		{
			System.out.println("currentMs:" + System.currentTimeMillis());
		}
	};
	
	public static Command<Object> reboot = new Command<Object>("reboot", "restart")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[0];
		}

		@Override
		public Object[] parse(String... args) 
		{
			return null;
		}

		@Override
		public void run(ParamList<Object> args) 
		{
			try
			{
				SelfCommandPrompt.reboot(new String[0]);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.err.println("unable to reboot");
			}
		}
	};
	
	public static Command<File> deepUnzip = new Command<File>(new String[]{"--nonAppUnzip", "--noExt"}, "deepUnzip", "deepUnarchive")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"Archive-File"};
		}

		@Override
		public File[] parse(String... inputs)
		{
			if(this.hasScanner(inputs))
			{
				return new File[]{this.nextFile("input archive file:")};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			File dir = params.get(0);
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.archiveExt);
			for(File file : files)
			{
				try
				{
					if(!params.hasFlag("nonAppUnzip"))
						JarUtil.deepUnzip(file, !params.hasFlag("noExt"));
					else
						JarUtil.deepNonAppUnzip(new DummyList<File>(), file);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	};
	
	public static Command<File> printMatchingArchives = new Command<File>("printMatchingArchives")
	{
		@Override
		public String[] displayArgs() 
		{
			return new String[]{"Archive-File","Archive-File || Dir"};
		}

		@Override
		public File[] parse(String... inputs) 
		{
			if(this.hasScanner(inputs))
			{
				return new File[]{this.nextFile("input archive:"), this.nextFile("input dir/archive to compare with:")};
			}
			return new File[]{DeDuperUtil.newFile(inputs[0]), DeDuperUtil.newFile(inputs[1])};
		}

		@Override
		public void run(ParamList<File> params) 
		{
			File file = params.get(0);
			ZipFile zip = JarUtil.getZipFile(file);
			File dir = params.get(1);
			List<File> files = DeDuperUtil.getDirFiles(dir, Main.archiveExt);
			boolean match = false;
			for(File f : files)
			{
				ZipFile zipCheck = JarUtil.getZipFile(f);
				boolean vanilla = JarUtil.getModdedFiles(zip, zipCheck).isEmpty();
				if(vanilla)
				{
					System.out.println("input Archive:" + file + " matched archive:" + f);
					match = true;
					break;
				}
			}
			if(!match)
				System.out.println("NO MATCHES FOUND:" + file);
		}
	};
	
	/**
	 * get a command based on it's id
	 */
	protected static void load(){}
	
}
