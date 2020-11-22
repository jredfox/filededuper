package jredfox.filededuper.command;

import java.io.File;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import jredfox.filededuper.Main;
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
	public abstract T[] getParams(String... cmdArgs);
	public abstract void run(T... args);
	
	public File nextFile(String msg)
	{
		this.print(msg);
		return new File(this.next()).getAbsoluteFile();
	}
	
	public String next()
	{
		return this.nextRaw(true);
	}
	
	public String next(String msg)
	{
		this.print(msg);
		return this.nextRaw(true);
	}
	
	public String nextRaw(boolean removeQuotes) 
	{
		String next = getScanner().nextLine().trim();
		return removeQuotes ? next.replace("\"", "") : next;
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
		this.print(msg);
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
		String nextLine = getScanner().nextLine();
		String[] args = DeDuperUtil.split(nextLine, ' ', '"', '"');
		fixArgs(args);
		Command c = Command.get(args[0]);
		if(c == null)
		{
			System.out.println("Invalid command \"" + args[0] + "\". Input new command or try using \"help\":");
			return nextCommand();
		}
		return args;
	}
	
	public static void fixArgs(String[] args) 
	{
		int index = 0;
		for(String s : args)
			args[index++] = s.trim().replace("\"", "");//trim before spacing so it preserves spaces
	}
	
	/**
	 * run a command starting with id and any command arguments that are required or optional can go next
	 */
	public static void run(String[] args)
	{
		Command c = Command.get(args[0]);
		boolean errored = false;
		long ms = System.currentTimeMillis();
		try
		{
			Object[] params = c.getParams(getCmdArgs(args));
			ms = System.currentTimeMillis();//try to make the time be before the running of the command if the exeption occurs during parsing the command it will display older ms
			c.run(params);
			System.gc();//clean memory
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			errored = true;
		}
		System.out.println("finished:" + c.id + " command " + (errored ? "with errors" : "successfully") + " in:" + (System.currentTimeMillis() - ms) + "ms");
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
