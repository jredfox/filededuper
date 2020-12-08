package jredfox.filededuper;

import java.io.IOException;
import java.util.Scanner;

import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		SelfCommandPrompt.runWithCMD("test_app", "Test APP", args);
		System.console().writer().write("edite me");
		System.console().writer().flush();
		Scanner scanner = new Scanner(System.in);
		String s = scanner.nextLine();
		System.out.println("\"" + s + "\"");
	}

}
