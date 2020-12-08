package jredfox.filededuper.command;

public class CommandParseException extends RuntimeException{
	
	public Throwable err;
	public CommandParseException(Throwable t)
	{
		this.err = t;
	}

}
