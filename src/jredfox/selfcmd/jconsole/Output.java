package jredfox.selfcmd.jconsole;

import java.io.IOException;
import java.io.OutputStream;
 
import javax.swing.JTextArea;
 
public class Output extends OutputStream {
    private JTextArea textArea;
     
    public Output(JTextArea textArea) {
        this.textArea = textArea;
    }
     
    @Override
    public void write(int b) throws IOException 
    {
        // redirects data to the text area
        textArea.append(String.valueOf((char)b));
        if(b == '\n')
        {
        	textArea.setCaretPosition(textArea.getDocument().getLength());
        	textArea.update(textArea.getGraphics());
        }
    }
}
