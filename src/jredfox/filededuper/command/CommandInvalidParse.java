package jredfox.filededuper.command;

public class CommandInvalidParse extends CommandInvalid{
	
	public String msg;
	public CommandInvalidParse(String id, String msg)
	{
		super("parsingException_" + id);
		this.msg = msg;
	}
	
	@Override
	public void run(ParamList<Object> params)
	{
		System.out.println(this.msg);
	}

}
