package jredfox.filededuper;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.JarUtil;

public class PointTimeEntry {
	
	public String programDir;
	public long minMs;
	public long maxMs;
	public long startMs;
	public String[] dirs;
	public Map<Long, Integer> times = new HashMap<>();//ConsistentMS, count
	public static final String defaultDir = "default";
	
	public PointTimeEntry(String[] dirs, ZipEntry entry, long time)
	{
		this(dirs, entry, JarUtil.getMinTime(time), JarUtil.getMaxTime(time) ,time);
	}
	
	public PointTimeEntry(String[] dirs, ZipEntry entry, long min, long max, long time)
	{
		this.dirs = dirs;
		this.programDir = this.fixProgramDir(entry);
		this.minMs = min;
		this.maxMs = max;
		this.startMs = time;
		this.add(entry, time);
	}
	
	public String fixProgramDir(ZipEntry entry) 
	{
		String name = entry.getName();
		if(!name.contains("/") && !entry.isDirectory())
			name = defaultDir;
		String fixedDir = null;
		for(String s : this.dirs)
		{
			if(name.startsWith(s))
			{
				fixedDir = s;
			}
		}
		return fixedDir;
	}
	
	public boolean add(ZipEntry entry, long num)
	{
		if(this.isWithinRange(entry, num))
		{
			int count = this.times.containsKey(num) ? this.times.get(num) : 0;
			this.times.put(num, count + 1);
			return true;
		}
		return false;
	}

	public boolean isWithinRange(ZipEntry entry, long num)
	{
		String pdir = this.fixProgramDir(entry);
		if(pdir == null)
			return false;
		return pdir.startsWith(this.programDir) && DeDuperUtil.isWithinRange(this.minMs, this.maxMs, num);
	}
	
	public int getPoints()
	{
		int points = -1;
		for(Integer i : this.times.values())
		{
			if(i > points)
				points = i;
		}
		return points;
	}
	
	@Override
	public String toString()
	{
		return this.programDir + " minMS:" + this.minMs + " maxMS:" + this.maxMs + " startTime:" + this.times;
	}

	/**
	 * returns the highest point value entry
	 */
	public Map.Entry<Long, Integer> getPointEntry() 
	{
		Map.Entry<Long, Integer> points = null;
		for(Map.Entry<Long, Integer> entry : this.times.entrySet())
		{
			int i = entry.getValue();
			if(points == null || i > points.getValue())
				points = entry;
		}
		return points;
	}
}
