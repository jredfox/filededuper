package jredfox.filededuper;

import java.io.File;
import java.io.IOException;

import jredfox.filededuper.util.DeDuperUtil;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		System.out.println(DeDuperUtil.getTrueName(new File("ziptest.cfg").getAbsoluteFile()));
	}

}
