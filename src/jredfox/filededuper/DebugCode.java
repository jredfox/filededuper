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
		File invalid = new File("an invalid path\\*/<>?\":|\\/t*e*s*t.cfg").getAbsoluteFile();
		File tst2 = new File("LPT0.png.txt").getAbsoluteFile();
		System.out.println(OSUtil.toOSFile(invalid) + "\n" + OSUtil.toOSFile(tst2));
	}

}
