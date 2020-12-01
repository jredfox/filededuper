package jredfox.selfcmd.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExeBuilder {
	
	public List<String> commands;
	
	public ExeBuilder()
	{
		this.commands = new ArrayList<>();
	}
	
	public void addCommand(String... commands)
	{
		for(String s : commands)
			this.add(s);
	}
	
	public void addCommand(Collection<String> col)
	{
		for(String s : col)
			this.add(s);
	}
	
	protected void add(String s)
	{
		this.commands.add(s);
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(String s : this.commands)
		{
			b.append(s + (index + 1 != this.commands.size() ? " " : ""));
			index++;
		}
		return b.toString();
	}

}
