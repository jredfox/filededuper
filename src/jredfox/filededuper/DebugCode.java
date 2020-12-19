package jredfox.filededuper;

import jredfox.filededuper.command.CommandOption;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		String cmd = "cd -a=\"v a l u e\"";
		String[] line = SelfCommandPrompt.parseCommandLine(cmd);
		CommandOption arg = new CommandOption("--stacktrace");
		System.out.println(arg);
	}

}
