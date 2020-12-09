package jredfox.filededuper.err;

import java.util.Map;
import java.util.Set;

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
		System.setErr(this.stream.old);
		this.stream = null;
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
	
	public Map<Thread, String> getErrMap()
	{
		return this.stream.errMap;
	}

}
