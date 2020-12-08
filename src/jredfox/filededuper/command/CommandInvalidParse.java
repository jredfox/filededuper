package jredfox.filededuper.command;

public class CommandInvalidParse extends CommandInvalid{
	
	public String msg;
	public CommandInvalidParse(String cmd, String msg)
	{
		super(cmd);
		this.msg = msg;
	}
	
	@Override
	public void run(ParamList<Object> params)
	{
		System.out.println(this.msg);
	}

}
