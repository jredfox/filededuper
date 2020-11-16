package jredfox.filededuper.archive;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchiveEntry {
	
	public ZipFile zip;
	public ZipEntry input;
	public ZipEntry output;
	
	public ArchiveEntry(ZipFile zip, ZipEntry input)
	{
		this(zip, input, input);
	}
	
	public ArchiveEntry(ZipFile zip, ZipEntry input, ZipEntry output)
	{
		this.zip = zip;
		this.input = input;
		this.output = output;
	}

}
