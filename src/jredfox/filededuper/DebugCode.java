package jredfox.filededuper;

import java.io.File;

import jredfox.selfcmd.util.OSUtil;

public class DebugCode {
	
	public static void main(String[] args)
	{	
		File test0 = new File("aux.class");
		File tst1 = OSUtil.toOSFile(new File("an invalid path\\*/<>?\":|\\/t*e*s*t.cfg\\\\")).getAbsoluteFile();
		File tst2 = OSUtil.toOSFile(new File("LPT0/LPT0.png.txt").getAbsoluteFile());
		File tst3 = OSUtil.toOSFile(new File("LPT0/config.cfg").getAbsoluteFile());
		System.out.println(test0 + "\n" + tst1 + "\n" + tst2 + "\n" + tst3);
	}

}
