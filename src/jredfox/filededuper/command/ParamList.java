package jredfox.filededuper.command;

import java.util.ArrayList;
import java.util.List;

public class ParamList<T> {
	
	public T[] params;
	public List<CommandOption> options = new ArrayList<>(1);
	
	@SuppressWarnings("unchecked")
	public ParamList(T... params)
	{
		this(new String[0], params);
	}
	
	@SuppressWarnings("unchecked")
	public ParamList(String[] oargs, T... params)
	{
		this.params = params;
		for(String o : oargs)
			this.options.add(new CommandOption(o));
	}
	
	@SuppressWarnings("unchecked")
	public <K> K get(int index)
	{
		return (K) this.params[index];
	}
	
	/**
	 * may return null if the option doesn't exist
	 */
	public CommandOption getOption(String id) 
	{
		for(CommandOption o : this.options)
			if(o.hasFlag(id))
				return o;
		return null;
	}
	
	public boolean hasFlag(String key)
	{
		for(CommandOption o : this.options)
			if(o.hasFlag(key))
				return true;
		return false;
	}
	
	public String getValue(String id)
	{
		CommandOption o = this.getOption(id);
		return o != null ? o.getValue() : "";
	}

	public String getValue(char c)
	{
		return this.getValue("" + c);
	}
	
	public boolean hasFlag(char c)
	{
		return this.hasFlag("" + c);
	}

}
