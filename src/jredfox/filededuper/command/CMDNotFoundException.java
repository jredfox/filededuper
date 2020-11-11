package jredfox.filededuper.command;

public class CMDNotFoundException extends RuntimeException{

	public CMDNotFoundException()
	{
		super();
	}
	
	public CMDNotFoundException(String msg)
	{
		super(msg);
	}
}
