package jredfox.filededuper.command.exception;

public class CommandRuntimeException extends CommandException{
	
	public CommandRuntimeException(String msg)
	{
		super(msg);
	}
	
	public CommandRuntimeException(Throwable t)
	{
		super(t);
	}

}
