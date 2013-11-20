package edu.harvard.hul.fdc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class LogWriter {

  private List<String> mLogs;

  public LogWriter() {
    mLogs = new ArrayList<String>();
  }

  public void submitLog(String log) {
    mLogs.add( log );
  }

  public void flush( String key ) {
    // TODO pick up file and write.
    StringBuffer buffer = new StringBuffer();
    newLine( buffer, "###" );
    newLine( buffer, "Comparing Base with " + key + " at " + new Date() );
    newLine( buffer, "");
    
    for (String log : mLogs) {
     newLine( buffer, log );
    }
    
    System.out.println( buffer.toString() );
    write(buffer);
    mLogs.clear();
  }
  
  private void write(StringBuffer buffer) {
    File logFile = new File(System.getProperty( "java.io.tmpdir" ) + File.separator + "/bbt-logs/log.txt");
    System.out.println("Writing logs to: " + logFile.getAbsolutePath());
    try {
      FileUtils.writeStringToFile(logFile, buffer.toString(), true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void newLine( StringBuffer buffer, String line ) {
    buffer.append( line + "\n" );
  }
}
