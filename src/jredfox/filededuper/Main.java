package jredfox.filededuper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.io.Files;

import jredfox.filededuper.command.CMDNotFoundException;
import jredfox.filededuper.command.Command;
import jredfox.filededuper.command.Commands;

public class Main {
	
	public static boolean errored;
	public static final String VERSION = "0.2.0";
	
	public static void main(String[] programArgs)
	{
		if(programArgs.length != 0)
		{
			Command cmd = Commands.getCommand(programArgs[0]);
			if(cmd == null)
				throw new CMDNotFoundException("Command not found Exception:" + programArgs[0]);
			Object[] cmdArgs = cmd.getParams();
			long ms = System.currentTimeMillis();
			cmd.run(cmdArgs);
			System.out.println("finished " + (errored ? "with errors" : "successfully in ") + (System.currentTimeMillis() - ms) + "ms");
		}
	}

}
