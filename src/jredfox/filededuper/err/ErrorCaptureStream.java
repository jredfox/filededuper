package jredfox.filededuper.err;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ErrorCaptureStream extends PrintStream {
	
	/**
	 * the current error. If it's not empty this means something printed to the error stream
	 */
	public volatile Map<Thread, ErrBuilder> errMap = new HashMap<>();
	/**
	 * whitelist of threads to listen to for errors
	 */
	public volatile Set<Thread> captures = new HashSet<>(2);
	/**
	 * the child or wrapped PrintStream object
	 */
	public PrintStream child;
	
	public ErrorCaptureStream()
	{
		this(System.err);
	}
	
	public ErrorCaptureStream(PrintStream out) 
	{
		super(out);
		this.child = out;
	}
	
    @Override
    public void print(boolean b) {
    	this.print(String.valueOf(b));
    }

    @Override
    public void print(char c) {
    	this.print(String.valueOf(c));
    }

    @Override
    public void print(int i) {
    	this.print(String.valueOf(i));
    }

    @Override
    public void print(long l) {
    	this.print( String.valueOf(l));
    }

    @Override
    public void print(float f) {
    	this.print(String.valueOf(f));
    }

    @Override
    public void print(double d) {
    	this.print(String.valueOf(d));
    }
    
    @Override
    public void print(Object obj) {
       this.print(String.valueOf(obj));
    }
    
    @Override
    public void print(char[] s) {
        super.print(s);
        notifyListener(String.valueOf(s));
    }
    
    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        notifyListener(String.valueOf(csq)); // TODO will need some special handling
        return super.append(csq, start, end);
    }

    @Override
    public void print(String s) {
        super.print(s);
        notifyListener(s);
    }
    
    @Override
    public void println() {
    	super.println();
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(boolean b) {
    	super.println(b);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(char x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(int x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(long x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(float x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(double x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(char[] x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(String x) {
    	super.println(x);
    	this.notifyListener("\n");
    }
    
    @Override
    public void println(Object x) {
    	super.println(x);
    	this.notifyListener("\n");
    }

    public synchronized void notifyListener(String str) 
    {
    	Thread t = Thread.currentThread();
    	if(this.captures.contains(t))
    	{
    		ErrBuilder builder = this.errMap.get(t);
    		if(builder == null)
    		{
    			builder = new ErrBuilder();
    			this.errMap.put(t, builder);
    		}
    		builder.append(str);
    	}
    }
    
	public void addCapture(Thread t)
	{
		this.captures.add(t);
	}
	
	public void removeCapture(Thread t) 
	{
		this.captures.remove(t);
		this.errMap.remove(t);
	}
	
	public boolean hasErr()
	{
		return !this.errMap.isEmpty();
	}
	
	public static class ErrBuilder 
	{
		protected StringBuilder builder = new StringBuilder();
		protected List<String> lines = new ArrayList<>();
		protected boolean endLineFeed;
		
		public ErrBuilder()
		{
			
		}

		public void append(String str)
		{
			for(int i=0; i < str.length(); i++)
			{
				String s = str.substring(i, i+1);
				if(s.equals("\n"))
				{
					String built = this.builder.toString();
					this.lines.add(built);
					this.builder = new StringBuilder();
					this.endLineFeed = true;
				}
				else
				{
					this.builder.append(s);
					this.endLineFeed = false;
				}
			}
		}
		
		/**
		 * get the lines from the error builder at the current time
		 */
		public List<String> getLines()
		{
			List<String> currentLines = new ArrayList<>(this.lines.size() + 1);
			for(String s : this.lines)
			{
				currentLines.add(s);
			}
			if(!this.builder.toString().isEmpty() || this.endLineFeed)
			{
				currentLines.add(this.builder.toString());
			}
			return currentLines;
		}
		
		public boolean hasError()
		{
			return !this.getLines().isEmpty();
		}
		
		@Override
		public String toString()
		{
			return this.getLines().toString();
		}
	}

}
