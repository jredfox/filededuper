package jredfox.filededuper.command;

public class ParamList<T> {
	
	public T[] args;
	
	@SuppressWarnings("unchecked")
	public ParamList(T... args)
	{
		this.args = args;
	}
	
	@SuppressWarnings("unchecked")
	public <K extends Object> K get(int index)
	{
		return (K) this.args[index];
	}

}
