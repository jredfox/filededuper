package jredfox.filededuper;

import jredfox.filededuper.command.Command;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		int index = 0;
		for(String s : args)
			System.out.println(index++ + "," + s);
	}

}
