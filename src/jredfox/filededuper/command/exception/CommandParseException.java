package jredfox.filededuper.command.exception;

public class CommandParseException extends RuntimeException{
	
	public Throwable err;
	public CommandParseException(Throwable t)
	{
		this.err = t;
	}

}
