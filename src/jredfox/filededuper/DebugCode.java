package jredfox.filededuper;

import jredfox.filededuper.command.OptionalArg;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		String cmd = "cd -a=\"v a l u e\"";
		String[] line = SelfCommandPrompt.parseCommandLine(cmd);
		OptionalArg arg = new OptionalArg("--v a l u e");
		System.out.println(arg);
	}

}
