package jredfox.filededuper.command;

public abstract class Command<T> {
	
	public String id;
	
	public Command(String cmd)
	{
		this.id = cmd;
		Commands.cmds.put(cmd, this);
	}
	
	public abstract T[] getParams(String... inputs);
	public abstract void run(T... agrs);

}
