package jredfox.filededuper.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class IOUtils {
	
	public static final byte[] buffer = new byte[1048576/2];
	
	public static void copy(InputStream in, OutputStream out) throws IOException
	{
		copy(in, out, true);
	}
	
	public static void copy(InputStream in, OutputStream out, boolean close) throws IOException
	{
		int length;
   	 	while ((length = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, length);
		}
   	 	if(close)
   	 	{
   	 		in.close();
   	 		out.close();
   	 	}
	}
	
	public static void closeQuietly(ZipFile zip)
	{
		close(zip, false);
	}
	
	public static void closeQuietly(InputStream stream)
	{
		close(stream, false);
	}
	
	public static void closeQuitely(OutputStream stream)
	{
		close(stream, false);
	}
	
	public static void close(OutputStream out)
	{
		close(out, true);
	}
	
	public static void close(InputStream in)
	{
		close(in, true);
	}

	public static void close(OutputStream out, boolean print)
	{
		try 
		{
			if(out != null)
				out.close();
		}
		catch (IOException e)
		{
			if(print)
				e.printStackTrace();
		}
	}
	
	public static void close(InputStream in, boolean print)
	{
		try 
		{
			if(in != null)
				in.close();
		}
		catch (IOException e)
		{
			if(print)
				e.printStackTrace();
		}
	}
	
	public static void close(ZipFile zip, boolean print)
	{
		try 
		{
			if(zip != null)
				zip.close();
		}
		catch (IOException e)
		{
			if(print)
				e.printStackTrace();
		}
	}
	
	/**
	 * Overwrites entire file default behavior no per line modification removal/addition
	 */
	public static void saveFileLines(List<String> list,File f,boolean utf8)
	{
		BufferedWriter writer = null;
		try
		{
			if(!utf8)
			{
				writer = new BufferedWriter(new FileWriter(f));
			}
			else
			{
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8 ) );
			}
			
			for(String s : list)
			{
				writer.write(s + "\r\n");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(writer != null)
			{
				try
				{
					writer.close();
				}
				catch(Exception e)
				{
					System.out.println("Unable to Close OutputStream this is bad");
				}
			}
		}
	}
	
	/**
	 * Equivalent to Files.readAllLines() but, works way faster
	 */
	public static List<String> getFileLines(File f)
	{
		return getFileLines(getReader(f));
	}
	
	public static List<String> getFileLines(String input) 
	{
		return getFileLines(getReader(input));
	}
	
	public static List<String> getFileLines(BufferedReader reader) 
	{
		List<String> list = null;
		try
		{
			list = new ArrayList();
			String s = reader.readLine();
			
			if(s != null)
			{
				list.add(s);
			}
			
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					list.add(s);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				try 
				{
					reader.close();
				} catch (IOException e) 
				{
					System.out.println("Unable to Close InputStream this is bad");
				}
			}
		}
		return list;
	}
	
	/**
	 * even though it's utf8 writing it's the fastest one I tried 5 different other options from different objects
	 */
	public static BufferedWriter getWriter(File f) throws FileNotFoundException, IOException
	{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
	}
	
	public static BufferedReader getReader(File f)
	{
		 try
		 {
			 return new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
		 }
		 catch(Throwable t)
		 {
			 return null;
		 }
	}
	
	public static BufferedReader getReader(String input)
	{
		return new BufferedReader(new InputStreamReader(DeDuperUtil.class.getClassLoader().getResourceAsStream(input)));
	}


}