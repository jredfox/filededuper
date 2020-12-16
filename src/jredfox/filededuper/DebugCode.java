package jredfox.filededuper;

import java.io.IOException;

import jredfox.filededuper.util.DeDuperUtil;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		for(String s : args)
			System.out.print(s + " ");
		System.out.println("\npost");
		args = getArgs(args);
		for(String s : args)
			System.out.print("\"" + s + "\"" + " ");
	}

	private static String[] getArgs(String[] args)
	{
		if(args.length != 0 && args[0].equals("background"))
		{
			String[] newArgs = new String[args.length - 1];
			System.arraycopy(args, 1, newArgs, 0, newArgs.length);
			return newArgs;
		}
		return new String[0];
	}

}
