package jredfox.filededuper.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import jredfox.filededuper.util.DeDuperUtil;

public abstract class Command<T>{
	
	public String id;//ids are lowercased enforced
	public String name;
	public List<String> ids;
	public List<String> names;
	public static Map<String, Command<?>> cmds = new TreeMap<>();
	public ParamList<T> params; 
	
	public Command(String... ids)
	{
		String base = ids[0];
		this.id = base.toLowerCase();
		this.name = ids[0];
		this.ids = new ArrayList<>(ids.length);
		this.names = new ArrayList<>(ids.length);
		for(String cmd : ids)
		{
			this.ids.add(cmd.toLowerCase());
			this.names.add(cmd);
		}
		this.register();
	}

	public abstract String[] displayArgs();
	public abstract T[] parse(String... args);
	public abstract void run(ParamList<T> args);
	
	public void run()
	{
		this.run(this.params);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void parseParamList(String[] args)
	{
		this.params = new ParamList(this.parse(args));
	}
	
	public boolean isCommand(String compareId)
	{
		return this.ids.contains(compareId);
	}
	
	public boolean isAliases(String compareId)
	{
		return this.ids.indexOf(compareId) > 0;
	}
	
	public File nextFile(String msg)
	{
		this.print(msg);
		return DeDuperUtil.newFile(this.next());
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
	
	protected void register() 
	{
		cmds.put(this.id, this);
	}
	
	public static Command<?> get(String id) 
	{
		id = id.toLowerCase();
		Commands.load();
		Command<?> fetched = cmds.get(id);//the hashing algorithm will make it faster
		Command<?> cmd = fetched != null ? fetched : getByAliases(id);
		return cmd != null ? cmd : new CommandInvalid(id);
	}

	public static Command<?> getByAliases(String id)
	{
		for(Command<?> c : cmds.values())
		{
			if(c.isAliases(id))
				return c;
		}
		return null;
	}
	
	public static Command<?> nextValidCommand()
	{
		Command<?> command = nextCommand();
		while(command instanceof CommandInvalid)
		{
			System.out.println("Invalid command \"" + command.id + "\". Input new command or try using \"help\":");
			command = nextCommand();
		}
		return command;
	}

	public static Command<?> nextCommand()
	{
		String[] args = fixArgs(DeDuperUtil.split(getScanner().nextLine(), ' ', '"', '"'));
		return fromArgs(args);
	}
	
	/**
	 * get the command by id and then parse args
	 */
	public static Command<?> fromArgs(String[] args) 
	{
		Command<?> c = Command.get(args[0]);
		if(c != null)
			c.parseParamList(getCmdArgs(args));
		return c;
	}
	
	protected static String[] fixArgs(String[] args) 
	{
		int index = 0;
		for(String s : args)
			args[index++] = s.trim().replace("\"", "");//trim before spacing so it preserves spaces
		return args;
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
	
	@Override
	public String toString()
	{
		return this.id;
	}
	
	@Override
	public boolean equals(Object other)
	{
		return this.id.equals( ((Command<?>)other).id );
	}
	
	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}

}
