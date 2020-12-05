package jredfox.filededuper;

import java.io.IOException;
import java.util.Scanner;

import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		args = SelfCommandPrompt.wrapWithCMD(args);
		for(String s : args)
			System.out.println(s);
	}

}
