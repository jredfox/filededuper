package jredfox.filededuper;

import java.io.IOException;

import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		args = SelfCommandPrompt.wrapWithCMD("testing>", args);
		for(String s : args)
			System.out.println(s);
	}

}
