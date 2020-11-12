package jredfox.filededuper.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import jredfox.filededuper.DeDuperUtil;
import jredfox.filededuper.config.csv.CSV;

public class Commands {
	
	public static HashMap<String, Command> cmds = new HashMap<>();
	
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
			List<File> files = DeDuperUtil.getDirFiles(dir, "*");//TODO: configurable extension for spreadsheet gen
			List<String> outStrs = new ArrayList<String>(files.size() + 10);
			Set<String> md5s = new HashSet<>(files.size() + 10);
			outStrs.add("#name, md5, sha-256, date-modified, boolean modified(jar only), path");
			for(File f : files)
			{
				String name = f.getName();
				String md5 = DeDuperUtil.getMD5(f);
				if(md5s.contains(md5))
					continue;
				String sha256 = DeDuperUtil.getSHA256(f);
				long lastModified = f.lastModified();
				String ext = DeDuperUtil.getExtension(f);
				if(ext.equals("jar"))
				{
					//TODO:
				}
				String path = DeDuperUtil.getRealtivePath(dir, f);
				outStrs.add(name + "," + md5 + "," + sha256 + "," + lastModified + "," + false + "," + path);
				md5s.add(md5);
			}
			File outputFile = new File(dir.getParent(), DeDuperUtil.getFileTrueName(dir) + "-output.csv");
			DeDuperUtil.saveFileLines(outStrs, outputFile, true);
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
			CSV output = new CSV(new File(origin.file.getParent(), DeDuperUtil.getFileTrueName(origin.file) + "-compared.csv"));
			output.add("#name, md5, sha-256, date-modified, boolean modified(jar only), path");
			origin.parse();
			compare.parse();
			
			//fetch the md5s from the origin
			Set<String> md5s = new HashSet(origin.lines.size() + 10);
			for(String[] line : origin.lines)
			{
				md5s.add(line[1]);
			}
			
			//inject any new entries
			for(String[] line : compare.lines)
			{
				String md5 = line[1];
				if(!md5s.contains(md5))
				{
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
	};
	
	public static Command<File> deepCompare = new Command<File>("deepCompare")
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
	};

	public static Command<File> checkJar = new Command<File>("checkJar")
	{
		@Override
		public File[] getParams(String... inputs)
		{
			if(inputs.length < 2)
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("input jar to check:");
				File toCheck = new File(scanner.nextLine());
				System.out.println("input jar of origin:");
				File origin = new File(scanner.nextLine());
				return new File[]{toCheck, origin};
			}
			return new File[]{new File(inputs[1]), new File(inputs[2])};
		}

		@Override
		public void run(File... args)
		{
			try
			{
			List<String> arr = new ArrayList();
			File toCheck = args[0];
			ZipFile zip = new ZipFile(toCheck);
			Enumeration entries = zip.entries();
			while(entries.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry) entries.nextElement();
				if(entry.isDirectory())
				{
					continue;
				}
				byte[] memory = extractInMemory(zip, entry);
				String md5 = DeDuperUtil.getMD5(new ByteArrayInputStream(memory));
				long lastModified = entry.getTime();
				arr.add(new File(entry.getName()).getName() + "," + md5 + "," + lastModified);
			}
			DeDuperUtil.saveFileLines(arr, new File(toCheck.getParent(), "test.csv"), true);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * get the byte[] in memory from a zip entry
	 * @throws IOException 
	 */
	public static byte[] extractInMemory(ZipFile zipFile, ZipEntry entry) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		InputStream zis = zipFile.getInputStream(entry);
		 int len;
         while ((len = zis.read(buffer)) > 0) 
         {
             out.write(buffer, 0, len);
         }
		return out.toByteArray();
	}
	
	
	
	
	
	
	
	
	
}
