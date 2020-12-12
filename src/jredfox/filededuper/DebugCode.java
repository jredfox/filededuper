package jredfox.filededuper;

import java.io.IOException;

import jredfox.filededuper.util.DeDuperUtil;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		boolean b = DeDuperUtil.isFileExt("test.cfg", new String[]{"noextension", "cfg"});
		System.out.println(b);
	}

}
