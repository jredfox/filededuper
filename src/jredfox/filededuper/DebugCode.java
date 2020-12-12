package jredfox.filededuper;

import java.io.IOException;

import jredfox.filededuper.err.ErrorCapture;
import jredfox.filededuper.err.ErrorCaptureStream.ErrorBuilder;

public class DebugCode {
	
	public static void main(String[] args) throws IOException
	{
		ErrorCapture capture = new ErrorCapture();
		capture.start();
		try
		{
			Integer.parseInt("a");
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		System.err.print("a");
		System.err.print("b\n");
		capture.stop();
		System.out.println();
		for(ErrorBuilder b : capture.getErrMap().values())
		{
			for(String s : b.getLines())
			{
				System.out.println("\"" + s  + "\"");
			}
		}
	}

}
