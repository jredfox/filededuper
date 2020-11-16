package jredfox.filededuper;

import java.util.ArrayList;
import java.util.List;

import jredfox.filededuper.util.DeDuperUtil;

public class PointTimeEntry {
	
	public String programDir;
	public long minMs;
	public long maxMs;
	public long startMs;
	public List<Long> times = new ArrayList();
	public static final String defaultDir = "default";
	
	public PointTimeEntry(String dir, long time)
	{
		this(dir, time - ( (1000L * 60L) * Main.time), time + ( (1000L * 60L) * Main.time) ,time);
	}
	
	public PointTimeEntry(String programDir, long min, long max, long time)
	{
		this.programDir = this.fixProgramDir(programDir);
		this.minMs = min;
		this.maxMs = max;
		this.startMs = time;
		this.times.add(time);
	}
	
	private String fixProgramDir(String dir) 
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

	public boolean isWithinRange(String programDir, long num)
	{
		programDir = this.fixProgramDir(programDir);
		if(programDir == null)
			return false;
		return programDir.startsWith(this.programDir) && DeDuperUtil.isWithinRange(this.minMs, this.maxMs, num);
	}
	
	@Override
	public String toString()
	{
		return this.programDir + " minMS:" + this.minMs + " maxMS:" + this.maxMs + " startTime:" + this.times.get(0);
	}
}
