package jredfox.filededuper.err;

import java.util.Map;
import java.util.Set;

import jredfox.filededuper.err.ErrorCaptureStream.ErrBuilder;

/**
 * this class allows you to detect if an error was handled and printed the stacktrace
 */
public class ErrorCapture {
	
	public ErrorCaptureStream stream;
	public boolean hasError;
	
	public ErrorCapture()
	{
		
	}
	
	public void start()
	{
		this.hasError = false;
		this.stream = new ErrorCaptureStream(System.err);
		System.setErr(this.stream);
		this.addCapture(Thread.currentThread());
	}
	
	public void stop()
	{
		this.hasError = this.stream.hasErr();
		System.setErr(this.stream.child);//don't set the capture's stream to null only the system's
	}
	
	public void addCapture(Thread t)
	{
		this.stream.addCapture(t);
	}
	
	public void removeCapture(Thread t)
	{
		this.stream.removeCapture(t);
	}
	
	public Set<Thread> getCaptures()
	{
		return this.stream.captures;
	}
	
	public Map<Thread, ErrBuilder> getErrMap()
	{
		return this.stream.errMap;
	}
	
	public boolean hasErr()
	{
		return this.hasError;
	}

}
