package jredfox.filededuper.config.csv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

public class CSV {
	
	public List<String[]> lines;
	public File file;
	
	public CSV(File file)
	{
		this(file, 10);//10 is the default array capacity
	}
	
	public CSV(File file, int initCapacity)
	{
		this.file = file;
		this.lines = new ArrayList<>(initCapacity);
	}
	
	public void parse()
	{
		List<String> fileLines = IOUtils.getFileLines(this.file);
		this.lines = new ArrayList<>(fileLines.size() + 10);
		for(String line : fileLines)
		{
			line = line.trim();
			if(line.isEmpty() || line.startsWith("#"))//skip invalid lines
				continue;
			this.add(line);
		}
	}
	
	public void add(String line)
	{
		this.lines.add(line.split(","));
	}
	
	public void add(int index, String line)
	{
		this.lines.add(index, line.split(","));
	}
	
	public String[] getLine(String key, int colum)
	{
		for(String[] line : this.lines)
		{
			String str = line[colum];
			if(str.equals(key))
			{
				return line;
			}
		}
		return null;
	}
	
	public boolean appendIf(String key, int colum, String value)
	{
		int count = 0;
		for(String[] line : this.lines)
		{
			String str = line[colum];
			if(str.equals(key))
			{
				this.lines.set(count, (toString(line) + "," + value).split(","));
				return true;
			}
			count++;
		}
		return false;
	}
	
	public void save()
	{
		List<String> writing = new ArrayList(this.lines.size());
		for(String[] line : this.lines)
			writing.add(this.toString(line));
		IOUtils.saveFileLines(writing, this.file, true);
	}

	public String toString(String[] line) 
	{
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(String s : line)
		{
			b.append(s + (index + 1 != line.length ? "," : "") );
			index++;
		}
		return b.toString();
	}

}
