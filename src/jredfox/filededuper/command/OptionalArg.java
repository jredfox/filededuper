package jredfox.filededuper.command;

import java.util.HashSet;
import java.util.Set;

import jredfox.filededuper.util.DeDuperUtil;

public class OptionalArg {
	
	public String dash;
	public String id;
	public String value;
	protected Set<String> flags = new HashSet<>();
	
	public OptionalArg(char c)
	{
		this("-" + c);
	}
	
	public OptionalArg(String arg)
	{
		if(!arg.contains("--"))
		{
			this.dash = "-";
			arg = arg.substring(1);
			StringBuilder b = new StringBuilder();
			for(int i=0;i<arg.length();i++)
			{
				String c = arg.substring(i, i + 1);
				if(c.equals(" "))
					throw new IllegalArgumentException("whitespace isn't allowed in flags");
				else if(c.equals("="))
					break;
				b.append(c);
				this.flags.add("" + c);
			}
			this.id = b.toString();
		}
		else if(arg.contains("--"))
		{
			this.dash = "--";
			arg = arg.substring(2);
			StringBuilder b = new StringBuilder();
			for(int i=0;i<arg.length();i++)
			{
				String c = arg.substring(i, i + 1);
				if(c.equals(" "))
					throw new IllegalArgumentException("whitespace isn't allowed in flags");
				else if(c.equals("="))
					break;
				b.append(c);
			}
			this.id = b.toString();
			this.flags.add(this.id);
		}
		else
		{
			throw new IllegalArgumentException("Optional Arg must be parsed with \"-\" or \"--\" input:\"" + arg + "\"");
		}
		
		if(arg.contains("="))
		{
			if(this.flags.size() > 1)
				throw new IllegalArgumentException("multiple flags cannot be assigned to one value");
			this.value = DeDuperUtil.splitFirst(arg, '=', '"', '"')[1];
		}
	}
	
	public boolean hasFlag(String key)
	{
		return this.flags.contains(key);
	}
	
	public boolean hasValue() 
	{
		return this.value != null;
	}
	
	public String getValue()
	{
		return this.value;
	}
	
	@Override
	public String toString()
	{
		return this.dash + this.id + (this.value != null ? "=" + this.value : "");
	}

}
