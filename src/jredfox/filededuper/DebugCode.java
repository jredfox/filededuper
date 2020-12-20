package jredfox.filededuper;

import java.io.File;

import jredfox.selfcmd.util.OSUtil;

public class DebugCode {
	
	public static void main(String[] args)
	{
//		String cmd = "cd -a=\"v a l u e\"";
//		String old = "cd -a=\"val ue\"";
//		String quoted = "cd -a=\"\\\"a quoted string\\\"\"";
//		for(String s : SelfCommandPrompt.parseCommandLine(quoted, '\\', '"'))
//			System.out.println(s);
		File tst1 = OSUtil.toOSFile(new File("an invalid path\\*/<>?\":|\\/t*e*s*t.cfg\\\\")).getAbsoluteFile();
		File tst2 = OSUtil.toOSFile(new File("LPT0/LPT0.png.txt")).getAbsoluteFile();
		File tst3 = OSUtil.toOSFile(new File("LPT0/config.cfg").getAbsoluteFile());
		System.out.println(tst1 + "\n" + tst2 + "\n" + tst3);
	}

}
