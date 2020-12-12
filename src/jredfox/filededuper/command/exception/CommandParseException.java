package jredfox.filededuper.command.exception;

public class CommandParseException extends RuntimeException{
	
	public CommandParseException(String msg)
	{
		super(msg);
	}
	
	public CommandParseException(Throwable t)
	{
		super(t);
	}

}
