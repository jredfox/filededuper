package jredfox.filededuper;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.filededuper.util.JarUtil;

public class PointTimeEntry {
	
	public String programDir;
	public long minMs;
	public long maxMs;
	public long startMs;
	public Map<Long, Integer> times = new HashMap<>();//ConsistentMS, count
	public static final String defaultDir = "default";
	
	public PointTimeEntry(String dir, long time)
	{
		this(dir, JarUtil.getMinTime(time), JarUtil.getMaxTime(time) ,time);
	}
	
	public PointTimeEntry(String programDir, long min, long max, long time)
	{
		this.programDir = this.fixProgramDir(programDir);
		this.minMs = min;
		this.maxMs = max;
		this.startMs = time;
		this.add(programDir, time);
	}
	
	public String fixProgramDir(String dir) 
	{
		if(!dir.contains("/"))
			dir = defaultDir;
		String fixedDir = null;
		for(String s : Main.programDirs) 
		{
			if(dir.startsWith(s))
			{
				fixedDir = s;
			}
		}
		return fixedDir;
	}
	
	public boolean add(String programDir, long num)
	{
		if(this.isWithinRange(programDir, num))
		{
			int count = this.times.containsKey(num) ? this.times.get(num) : 0;
			this.times.put(num, count + 1);
			return true;
		}
		return false;
	}

	public boolean isWithinRange(String programDir, long num)
	{
		programDir = this.fixProgramDir(programDir);
		if(programDir == null)
			return false;
		return programDir.startsWith(this.programDir) && DeDuperUtil.isWithinRange(this.minMs, this.maxMs, num);
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
