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

	public static Command<Object> getCommandFromConsole(String strCommand)
	{
		Command c = Commands.getCommand(strCommand);
		while(c == null)
		{
			System.out.println("Invalid command \"" + strCommand + "\". Input new command or try using \"help\":");
			c = Commands.getCommand(getScanner().nextLine());
		}
		return c;
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

}
