package jredfox.filededuper.archive;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Zip extends ZipFile{
	
	public File file;
	public Zip(File file) throws ZipException, IOException 
	{
		super(file);
		this.file = file;
	}

}
