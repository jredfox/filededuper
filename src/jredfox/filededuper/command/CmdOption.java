package jredfox.filededuper.command;

import java.util.HashSet;
import java.util.Set;

import jredfox.filededuper.command.exception.CommandParseException;
import jredfox.filededuper.util.DeDuperUtil;

public class CmdOption {
	
	public String id;
	public String value;
	protected Set<CmdOption> subs = new HashSet<>(0);
	
	public CmdOption(char c)
	{
		this("" + c);
	}
	
	public CmdOption(String id)
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
				if(s.equals("=") && !this.subs.isEmpty())
					throw new IllegalArgumentException("combined flags cannot be assigned a value!");
				this.subs.add(new CmdOption("-" + s));
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
	
	public boolean hasFlag(char c)
	{
		return this.hasFlag("" + c);
	}

	public boolean hasFlag(String k)
	{
		return this.id.equalsIgnoreCase(k) || this.hasSubFlags(k);
	}

	protected boolean hasSubFlags(String k) 
	{
		for(CmdOption o : this.subs)
		{
			if(o.id.equalsIgnoreCase(k))
				return true;
		}
		return false;
	}
	
	public String getValue()
	{
		return this.value;
	}
	
	@Override
	public String toString()
	{
		if(this.id.length() > 1)
			return "--" + this.id + (this.value != null ? ("=" + this.value) : "");
		StringBuilder b = new StringBuilder();
		b.append("-" + this.id);
		for(CmdOption o : this.subs)
			b.append(o.id);
		return b.toString();
	}

}
