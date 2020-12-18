package jredfox.filededuper.exception;

public class CompileTimeException extends RuntimeException {
	
	public CompileTimeException(Exception e)
	{
		super(e);
	}
	
	public CompileTimeException(String msg)
	{
		super(msg);
	}

}
