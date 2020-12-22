package jredfox.filededuper.command;

import java.util.HashSet;
import java.util.Set;

import jredfox.filededuper.command.exception.CommandParseException;
import jredfox.filededuper.util.DeDuperUtil;

public class CommandOption {
	
	protected String id;
	protected String value;
	protected Set<CommandOption> subs = new HashSet<>(0);
	protected Set<CommandOption> incompats = new HashSet<>(0);
	
	public CommandOption(char c)
	{
		this("" + c);
	}
	
	public CommandOption(String[] incompats, String id)
	{
		this(id);
		this.incompats = new HashSet<>(incompats.length);
		for(String option : incompats)
			this.incompats.add(new CommandOption(option));
	}
	
	public CommandOption(String id)
	{
		int dashes = this.getDashes(id);
		if(dashes == 0)
			throw new CommandParseException("CmdOption must contain a dash!");
		else if (dashes == 1)
		{
			id = id.substring(1);
			this.id = "" + id.charAt(0);
			for(int i=1; i < id.length(); i++)
			{
				String s = id.substring(i, i + 1);
				if(s.equals("="))
				{
					if(!this.subs.isEmpty())
						throw new IllegalArgumentException("combined flags cannot be assigned a value!");
					break;
				}
				else if(this.hasFlag(s))
					throw new IllegalArgumentException("duplicate flag:\"" + s + "\"");
				this.subs.add(new CommandOption("-" + s));
			}
		}
		else if(dashes == 2)
		{
			id = id.substring(2);
			StringBuilder b = new StringBuilder();
			for(int i=0; i < id.length(); i++)
			{
				String s = id.substring(i, i + 1);
				if(s.equals("="))
					break;
				b.append(s);
			}
			this.id = b.toString();
		}
		
		if(id.contains("="))
		{
			this.value = DeDuperUtil.splitFirst(id, '=', '"', '"')[1];
		}
	}
	
	public boolean isIncompat(CommandOption other)
	{
		for(CommandOption o : this.incompats)
			if(o.hasFlag(other))
				return true;
		return false;
	}
	
	public void addIncompat(CommandOption o)
	{
		this.incompats.add(o);
	}
	
	public void removeIncompat(CommandOption o)
	{
		this.incompats.remove(o);
	}
	
	public Set<CommandOption> getIncompats()
	{
		return this.incompats;
	}
	
	public boolean hasFlag(char c)
	{
		return this.hasFlag("" + c);
	}

	public boolean hasFlag(String k)
	{
		if(id.startsWith("-"))
			throw new IllegalArgumentException("do not input unparsed command option keys:" + id);
		return this.id.equalsIgnoreCase(k) || this.hasSubFlags(k);
	}

	public boolean hasFlag(CommandOption option) 
	{
		return this.hasFlag(option.id) || this.hasSubFlags(option);
	}
	
	public String getValue()
	{
		return this.value;
	}
	
	public boolean hasValue()
	{
		return this.value != null;
	}
	
	protected int getDashes(String str) 
	{
		int count = 0;
		for(char c : str.toCharArray())
		{
			if(c == '-')
				count++;
			else
				break;
		}
		return count;
	}

	protected boolean hasSubFlags(String k) 
	{
		for(CommandOption o : this.subs)
		{
			if(o.id.equalsIgnoreCase(k))
				return true;
		}
		return false;
	}

	protected boolean hasSubFlags(CommandOption option) 
	{
		for(CommandOption o : option.subs)
		{
			if(this.hasFlag(o.id))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		String value = (this.value != null ? ("=" + this.value) : "");
		if(this.id.length() > 1)
			return "--" + this.id + value;
		StringBuilder b = new StringBuilder();
		b.append("-" + this.id);
		for(CommandOption o : this.subs)
			b.append(o.id);
		b.append(value);
		return b.toString();
	}

	public String toFancyString() 
	{
		String value = (this.value != null ? ("=" + (this.value.isEmpty() ? "value" : this.value) ) : "");
		if(this.id.length() > 1)
			return "--" + this.id + value;
		StringBuilder b = new StringBuilder();
		b.append("-" + this.id);
		for(CommandOption o : this.subs)
			b.append(", -" + o.id);
		b.append(value);
		return b.toString();
	}
	
	@Override
	public int hashCode()
	{
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof CommandOption))
			return false;
		return this.hasFlag((CommandOption)obj);
	}
}
