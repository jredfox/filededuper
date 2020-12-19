package jredfox.filededuper;

import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		String cmd = "cd -a=\"v a l u e\"";
		String old = "cd -a=\"val ue\"";
		String quoted = "cd -a=\"\\\"a quoted string\\\"\"";
		for(String s : SelfCommandPrompt.parseCommandLine(quoted, '\\', '"'))
			System.out.println(s);
	}

}
