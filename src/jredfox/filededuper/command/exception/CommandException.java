package jredfox.filededuper.command.exception;

public class CommandException extends RuntimeException{
	
	public Throwable cause;
	
	public CommandException(String msg)
	{
		super(msg);
	}
	
	public CommandException(Throwable t)
	{
		this.cause = t;
	}
	
	public Throwable getCause()
	{
		return this.cause;
	}

}
