package jredfox.filededuper;

import java.io.IOException;
import java.util.Scanner;

import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		String str = "\"" + "\\\"a \\\\\"quoted test\\\"" + "\"";
		System.out.println(str);
        System.out.println(SelfCommandPrompt.parseQuotes(str, '"', '"'));
	}

}
