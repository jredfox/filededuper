package jredfox.filededuper;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import jredfox.filededuper.util.DeDuperUtil;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		File file = new File("test.cfg").getAbsoluteFile();
		System.out.println(DeDuperUtil.getRealtivePath(file.getParentFile(), file));
	}

}
