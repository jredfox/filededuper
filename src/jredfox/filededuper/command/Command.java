package jredfox.filededuper.command;

import java.io.File;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import jredfox.filededuper.ErrorCaptureStream;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.selfcmd.SelfCommandPrompt;

public abstract class Command<T> {
	
	public String id;//ids are lowercased enforced
	public String name;
	public List<String> ids;
	public List<String> names;
	public static Map<String, Command<?>> cmds = new TreeMap<>();
	public ParamList<T> params;
	public boolean err;
	
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
	public void parseParamList(String[] args) throws CommandParseException
	{
		try
		{
			this.setErr(false);
			T[] params = this.parse(args);
			this.params = new ParamList(params);
		}
		catch(Exception e)
		{
			this.setErr(true);
			throw new CommandParseException(e);
		}
	}
	
	protected void register() 
	{
		cmds.put(this.id, this);
	}
	
	public boolean isCommand(String compareId)
	{
		return this.ids.contains(compareId);
	}
	
	public boolean isAliases(String compareId)
	{
		return this.ids.indexOf(compareId) > 0;
	}
	
	public boolean isErr()
	{
		return this.err;
	}
	
	public void setErr(boolean err)
	{
		this.err = err;
	}
	
	public static Scanner getScanner()
	{
		return SelfCommandPrompt.scanner;
	}
	
	public boolean hasScanner(String... inputs)
	{
		return inputs.length == 0;
	}
	
	public String next()
	{
		return getScanner().nextLine().trim().replace("\"", "");
	}
	
	public String next(String msg)
	{
		this.print(msg);
		return this.next();
	}
	
	public Long nextLong(String msg)
	{
		this.print(msg);
		return Long.parseLong(this.next());
	}
	
	public Integer nextInt(String msg)
	{
		this.print(msg);
		return Integer.parseInt(this.next());
	}
	
	public Short nextShort(String msg)
	{
		this.print(msg);
		return Short.parseShort(this.next());
	}
	
	public Byte nextByte(String msg)
	{
		this.print(msg);
		return Byte.parseByte(this.next());
	}
	
	public Double nextDouble(String msg)
	{
		this.print(msg);
		return Double.parseDouble(this.next());
	}
	
	public Float nextFloat(String msg)
	{
		this.print(msg);
		return Float.parseFloat(this.next());
	}
	
	public BigDecimal nextBigDecimal(String msg)
	{
		this.print(msg);
		return new BigDecimal(this.next());
	}
	
	public File nextFile(String msg)
	{
		this.print(msg);
		return DeDuperUtil.newFile(this.next());
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
		return this.id.equals( ((Command<?>)other).id );
	}
	
	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}
	
	public static Command<?> get(String id) 
	{
		id = id.toLowerCase();
		Commands.load();
		Command<?> fetched = cmds.get(id);//the hashing algorithm will make it faster
		Command<?> cmd = fetched != null ? fetched : getByAliases(id);
		Command<?> actual = cmd != null ? cmd : new CommandInvalid(id);
		actual.setErr(false);
		return actual;
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
			command.run();
			command = nextCommand();
		}
		return command;
	}

	public static Command<?> nextCommand()
	{
		String[] args = SelfCommandPrompt.parseCommandLine(getScanner().nextLine());
		if(args.length == 0)
			args = new String[]{""};
		return fromArgs(args);
	}
	
	/**
	 * get the command by id and then parse args
	 */
	public static Command<?> fromArgs(String[] args) 
	{
		Command<?> c = Command.get(args[0]);
		if(c != null) 
		{
			try {
				c.parseParamList(getCmdArgs(args));
			}
			catch(CommandParseException e) {
				return new CommandInvalidParse(DeDuperUtil.toString(args, " "), " Invalid Param arguments for command:\"" + c.name + "\"" + ", ParamsList:(" + DeDuperUtil.toString(c.displayArgs(), ", ") + ")");
			}
		}
		return c;
	}

	protected static String[] getCmdArgs(String[] args)
	{
		if(args.length > 0)
		{
			String[] actualArgs = new String[args.length - 1];
			System.arraycopy(args, 1, actualArgs, 0, actualArgs.length);
			return actualArgs;
		}
		return new String[]{""};
	}

	/**
	 * parse a command print results
	 */
	public static void run(String[] args)
	{
		Command<?> command = Command.fromArgs(args);
		run(command);
	}
	
	/**
	 * run a command print results
	 */
	public static void run(Command<?> command)
	{
		long ms = System.currentTimeMillis();
		PrintStream old = System.err;
		ErrorCaptureStream errTst = new ErrorCaptureStream(System.err);
		System.setErr(errTst);
		boolean errored = false;
		try
		{
			command.run();
			errored = command.isErr();
		}
		catch(Exception e)
		{
			e.printStackTrace();
//			errored = true;//no longer needed since I have the error capture stream
		}
		finally
		{
			if(!errTst.currentErr.isEmpty())
				errored = true;
			System.setErr(old);
		}
		if(!(command instanceof CommandInvalid))
			System.out.println("finished:" + command.name + " command " + (errored ? "with errors" : "successfully") + " in:" + (System.currentTimeMillis() - ms) + "ms");
		command.setErr(false);
	}

}
