package jredfox.filededuper;

import jredfox.filededuper.command.CmdOption;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		String cmd = "cd -a=\"v a l u e\"";
		String[] line = SelfCommandPrompt.parseCommandLine(cmd);
		CmdOption arg = new CmdOption("--stacktrace");
		System.out.println(arg.hasFlag("stacktrace"));
	}

}
