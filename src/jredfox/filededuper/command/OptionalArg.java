package jredfox.filededuper.command;

import java.util.ArrayList;
import java.util.List;

import jredfox.filededuper.util.DeDuperUtil;

public class OptionalArg {
	
	public String id;
	public String value;
	
	private OptionalArg(char c)
	{
		this("" + c);
	}
	
	private OptionalArg(String str)
	{
		this.id = str;
		if(str.contains("="))
			this.value = DeDuperUtil.splitFirst(str, '=')[1];
	}
	
	public List<OptionalArg> parse(String str)
	{
		List<OptionalArg> options = new ArrayList<>();
		if(str.contains("-"))
		{
			str = str.substring(1);
			for(char c : str.toCharArray())
				options.add(new OptionalArg(c));
		}
		else
		{
			options.add(new OptionalArg(str.substring(2)));
		}
		return options;
	}

}
