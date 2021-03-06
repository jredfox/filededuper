package jredfox.filededuper.command;

public class CommandInvalid extends Command<Object>{
	
	public CommandInvalid(String... args)
	{
		super(args);
		this.id = args[0];
	}
	
	@Override
	public String[] displayArgs()
	{
		return new String[0];
	}

	@Override
	public Object[] parse(String... args)
	{
		return null;
	}

	@Override
	public void run(ParamList<Object> args) 
	{
		System.out.println("Invalid command \"" + this.id + "\". Input new command or try using \"help\":");
	}
	
	@Override
	protected void register() 
	{
		
	}

}
