package jredfox.filededuper.config.csv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.IOUtils;

public class CSV {
	
	public List<String[]> lines = new ArrayList<>();
	public File file;
	
	public CSV(File file)
	{
		this.file = file;
	}
	
	public void parse()
	{
		List<String> fileLines = IOUtils.getFileLines(this.file);
		this.lines = new ArrayList<>(fileLines.size() + 10);
		for(String line : fileLines)
		{
			if(line.trim().isEmpty() || line.startsWith("#"))//skip invalid lines
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
