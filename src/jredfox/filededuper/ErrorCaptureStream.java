package jredfox.filededuper;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class ErrorCaptureStream extends PrintStream {
	
	/**
	 * the current error. If it's not empty this means something printed to the error stream
	 */
	public volatile String currentErr = "";
	/**
	 * whitelist of threads to listen to for errors
	 */
	public volatile Set<Thread> captures = new HashSet<>(1);
	public OutputStream old;
	
	public ErrorCaptureStream(OutputStream out) 
	{
		super(out);
		this.old = out;
		this.captures.add(Thread.currentThread());
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
    public void print(String s) {
        super.print(s);
        notifyListener(s);
    }
    
    @Override
    public void print(char[] s) {
        super.print(s);
        notifyListener(String.valueOf(s));
    }

    public void notifyListener(String str) 
    {
    	if(this.captures.contains(Thread.currentThread()))
    		this.currentErr = str;
    }

}
