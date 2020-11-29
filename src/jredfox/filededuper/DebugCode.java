package jredfox.filededuper;

import jredfox.selfcmd.jconsole.JConsole;

public class DebugCode {
	
	public static void main(String[] args)
	{
		JConsole console = new JConsole("Virtual Command Prompt", true)
		{
			@Override
			public boolean isJavaCommand(String[] command) 
			{
				return false;
			}

			@Override
			public boolean shutdown()
			{
				return true;
			}
		};
		console.setEnabled(true);
	}

}
