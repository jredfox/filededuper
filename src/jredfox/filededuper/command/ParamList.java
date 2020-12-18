package jredfox.filededuper.command;

import java.util.ArrayList;
import java.util.List;

import jredfox.filededuper.command.exception.CommandParseException;

public class ParamList<T> {
	
	public T[] params;
	public List<OptionalArg> oparams;
	
	@SuppressWarnings("unchecked")
	public ParamList(String[] oargs, T... params) throws CommandParseException
	{
		this(params);
		this.oparams = new ArrayList<>();
		for(String oarg : oargs) 
			this.oparams.add(new OptionalArg(oarg));
	}
	
	@SuppressWarnings("unchecked")
	public ParamList(T... params)
	{
		this.params = params;
	}
	
	@SuppressWarnings("unchecked")
	public <K> K get(int index)
	{
		return (K) this.params[index];
	}
	
	public OptionalArg get(String key)
	{
		for(OptionalArg o : this.oparams)
		{
			
		}
		return null;//TODO:
	}
	
	public boolean getFlag(String key)
	{
		return false;//TODO:
	}
	
	public boolean getFlag(char c)
	{
		return false;//TODO:
	}

}
