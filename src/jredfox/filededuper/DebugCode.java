package jredfox.filededuper;

import java.util.Scanner;

import jredfox.filededuper.command.Command;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		String appId = DebugCode.class.getName().replaceAll("\\.", "/");
		SelfCommandPrompt.runWithCMD(appId, appId, args);
        System.out.print("test");
        String a = new Scanner(System.in).nextLine();
        System.out.println("out:\"" + a + "\"");
	}

}
