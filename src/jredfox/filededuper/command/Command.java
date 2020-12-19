package jredfox.filededuper.command;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import jredfox.filededuper.command.exception.CommandParseException;
import jredfox.filededuper.err.ErrorCapture;
import jredfox.filededuper.util.DeDuperUtil;
import jredfox.selfcmd.SelfCommandPrompt;

public abstract class Command<T> {
	
	public String id;//ids are lowercased enforced
	public String name;
	public List<String> ids;
	public List<String> names;
	public ParamList<T> params;
	public boolean hasError;
	public List<CommandOption> allowedOptions = new ArrayList<>(0);
	
	public static Map<String, Command<?>> cmds = new TreeMap<>();
	
	public Command(String[] options, String... ids)
	{
		this(ids);
		this.allowedOptions = new ArrayList<>(options.length);
		for(String s : options)
			this.allowedOptions.add(new CommandOption(s));
	}
	
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
	public abstract T[] parse(String... inputs);
	public abstract void run(ParamList<T> params);
	
	public void run()
	{
		this.setError(false);
		this.run(this.params);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void parseParamList(String[] args) throws CommandParseException
	{
		try
		{
			String[] paramArgs = this.getParamArgs(args);
			String[] optionArgs = this.getOptionalArgs(args);
			List<CommandOption> options = new ArrayList(optionArgs.length);
			for(String o : optionArgs)
			{
				CommandOption option = new CommandOption(o);
				if(!this.isOptionValid(option))
					throw new CommandParseException("optional command argument is maulformed or invalid:" + option);
				options.add(option);
			}
			T[] params = this.parse(paramArgs);
			this.params = new ParamList(options, params);
			this.setError(false);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new CommandParseException(e);
		}
	}
	
	public boolean isOptionValid(CommandOption option)
	{
		for(CommandOption o : this.allowedOptions)
		{
			if(o.hasFlag(option) && (o.hasValue() == option.hasValue()))
			{
				return true;
			}
		}
		return false;
	}

	public String[] getOptionalArgs(String[] args) 
	{
		List<String> list = new ArrayList<>(2);
		for(String s : args)
		{
			if(s.startsWith("-"))
				list.add(s);
		}
		return DeDuperUtil.toArray(list, String.class);
	}
	
	public String[] getParamArgs(String[] args) 
	{
		List<String> list = DeDuperUtil.asList(args);
		if(list.get(0).equalsIgnoreCase(this.id))
			list.remove(0);
		Iterator<String> it = list.iterator();
		while(it.hasNext())
		{
			String s = it.next();
			if(s.startsWith("-"))
				it.remove();
		}
		return DeDuperUtil.toArray(list, String.class);
	}

	protected void register() 
	{
		cmds.put(this.id, this);
	}
	
	public List<CommandOption> getOptions()
	{
		return this.allowedOptions;
	}
	
	public boolean isCommand(String compareId)
	{
		return this.ids.contains(compareId);
	}
	
	public boolean isAliases(String compareId)
	{
		return this.ids.indexOf(compareId) > 0;
	}
	
	public boolean hasError()
	{
		return this.hasError;
	}
	
	public void setError(boolean err)
	{
		this.hasError = err;
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
				c.parseParamList(args);
			}
			catch(CommandParseException e) {
				return new CommandInvalidParse(c.id, "Invalid Param arguments for command:\"" + c.name + "\"" + ", ParamsList:(" + DeDuperUtil.toString(c.displayArgs(), ", ") + ")");
			}
		}
		return c;
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
		ErrorCapture capture = new ErrorCapture();
		capture.start();
		boolean errored = false;
		try
		{
			command.run();
			errored = command.hasError();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			capture.stop();
			if(capture.hasError)
				errored = true;
			System.gc();//cleanup the garbage from the ErrorCapture and any data from the running command
		}
		if(!(command instanceof CommandInvalid))
			System.out.println("finished:" + command.name + " command " + (errored ? "with errors" : "successfully") + " in:" + (System.currentTimeMillis() - ms) + "ms");
	}

}
