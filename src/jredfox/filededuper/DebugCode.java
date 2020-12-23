package jredfox.filededuper;

import java.io.File;
import java.util.Set;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.selfcmd.util.OSUtil;

public class DebugCode {
	
	public static volatile Set<String> hashes;
	public static void main(String[] args)
	{	
		hashes = DeDuperUtil.getHashes(new File("user.dir"));
		File tst0 = OSUtil.toWinFile(new File("aux.class"));
		File tst1 = OSUtil.toOSFile(new File("an invalid path\\*/<>?\":|\\/t*e*s*t.cfg\\\\")).getAbsoluteFile();
		File tst2 = OSUtil.toOSFile(new File("LPT0/LPT0.png.txt").getAbsoluteFile());
		File tst3 = OSUtil.toOSFile(new File("LPT0/config.cfg").getAbsoluteFile());
		System.out.println(tst0 + "\n" + tst1 + "\n" + tst2 + "\n" + tst3);
	}
}
