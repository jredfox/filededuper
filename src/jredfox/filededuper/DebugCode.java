package jredfox.filededuper;

import jredfox.filededuper.command.Command;
import jredfox.selfcmd.SelfCommandPrompt;

public class DebugCode {
	
	public static void main(String[] args)
	{
		String[] split = Command.fixArgs(SelfCommandPrompt.split("\"\\\"this is a test\\\"\" \"index_2\"", ' ', '"', '"'));
		String tst = "\\\"test\\\"";
		System.out.println(tst);
		System.out.println(parseQuotes(tst, '"', '"'));
	}
	
	public static String parseQuotes(String s, char lq, char rq) 
	{
		return parseQuotes(s, 0, lq, rq);
	}

	public static String parseQuotes(String s, int index, char lq, char rq)
	{
		StringBuilder builder = new StringBuilder();
		char prev = 'a';
		int count = 0;
		boolean hasQuote = hasQuote(s.substring(index, s.length()), lq);
		for(int i=index;i<s.length();i++)
		{
			String c = s.substring(i, i + 1);
			boolean escaped = prev == '\\';
			if(hasQuote && !escaped && (count == 0 && c.equals("" + lq) || count == 1 && c.equals("" + rq)))
			{
				count++;
				if(count == 2)
					break;
				prev = c.charAt(0);//set previous before skipping
				continue;
			}
			if(!hasQuote || count == 1)
			{
				builder.append(c);
			}
			prev = c.charAt(0);//set the previous char here
		}
		return lq == rq ? builder.toString().replaceAll("\\\\" + lq, "" + lq) : builder.toString().replaceAll("\\\\" + lq, "" + lq).replaceAll("\\\\" + rq, "" + rq);
	}

	public static boolean hasQuote(String str, char lq) 
	{
		char prev = 'a';
		for(char c : str.toCharArray())
		{
			if(c == lq && prev != '\\')
				return true;
			prev = c;
		}
		return false;
	}

}
