package jredfox.filededuper;

import java.io.File;

import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		SelfCommandPrompt.getClassPath(new File[]{new File("a"), new File("b")});
	}

}
