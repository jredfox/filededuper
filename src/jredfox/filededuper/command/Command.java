package jredfox.filededuper.command;

import java.io.File;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import jredfox.filededuper.util.DeDuperUtil;

public abstract class Command<T> {
	
	public String id;
	public static Map<String, Command<?>> cmds = new TreeMap<>();
	
	public Command(String cmd)
	{
		this.id = cmd;
		cmds.put(cmd, this);
	}

	public abstract String[] getArgs();
	public abstract T[] getParams(String... inputs);
	public abstract void run(T... args);
	
	public File nextFile(String msg)
	{
		this.print(msg);
		return new File(this.next());
	}
	
	public String next() 
	{
		return getScanner().nextLine();
	}
	
	public boolean hasScanner(String... inputs)
	{
		return inputs.length == 0;
	}
	
	public static Scanner getScanner()
	{
		return DeDuperUtil.scanner;
	}

	public Long nextLong(String msg)
	{
		return Long.parseLong(this.next());
	}
	
	public void print(String msg) 
	{
		if(!msg.isEmpty())
			System.out.println(msg);
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}
	
	@Override
	public boolean equals(Object other)
	{
		return this.id.equals( ((Command)other).id );
	}
	
	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}
	
	public static Command<?> get(String id) 
	{
		return Commands.get(id);
	}

	public static String[] nextCommand() 
	{
		String[] args = getScanner().nextLine().split(" ");
		Command c = Command.get(args[0]);
		if(c == null)
		{
			System.out.println("Invalid command \"" + args[0] + "\". Input new command or try using \"help\":");
			return nextCommand();
		}
		return args;
	}
	
	/**
	 * run a command starting with id and any command arguments that are required or optional can go next
	 */
	public static void run(String[] args)
	{
		Command c = Command.get(args[0]);
		Object[] params = c.getParams(getCmdArgs(args));
		long ms = System.currentTimeMillis();
		c.run(params);
		System.gc();//clean memory
		System.out.println("finished:" + c.id + " in:" + (System.currentTimeMillis() - ms) + "ms");
	}

	protected static String[] getCmdArgs(String[] args)
	{
		if(args.length > 0)
		{
			String[] actualArgs = new String[args.length - 1];
			System.arraycopy(args, 1, actualArgs, 0, actualArgs.length);
			return actualArgs;
		}
		return args;
	}

}
