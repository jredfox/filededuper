package jredfox.filededuper.command.exception;

public class CommandParseException extends CommandException{
	
	public CommandParseException(String msg)
	{
		super(msg);
	}
	
	public CommandParseException(Throwable t)
	{
		super(t);
	}

}
