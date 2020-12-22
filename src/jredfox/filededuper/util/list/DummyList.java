package jredfox.filededuper.util.list;

import java.util.ArrayList;
import java.util.Collection;

public class DummyList<T> extends ArrayList<T>{

	public DummyList()
	{
		
	}
	
	@Override
	public boolean add(T t)
	{
		return false;
	}
	
	@Override
	public void add(int index, T t)
	{
		
	}
	
	@Override
	public boolean addAll(Collection<? extends T> col)
	{
		return false;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends T> col)
	{
		return false;
	}
	
	@Override
	public boolean isEmpty()
	{
		return true;
	}

}
