package edu.harvard.hul.fbt;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import edu.harvard.hul.fbt.cli.CLI;

public class Controller {

  private CLI mCLI;

  private String[] mInput;

  public Controller(CLI cli) {
    mCLI = cli;
  }

  public void setInput( String... args ) {
    mInput = args;
  }

  public int run() {
    
    if ( mInput == null ) {
      // TODO proper logging
      printHelp();
      return 1; // TODO proper exit code;
    }

    try {

      mCLI.parse( mInput );

    } catch ( ParseException e ) {

      printHelp();
      
      int exitCode = e.getMessage().equals( "HELP" ) ? 0 : 2;

      return exitCode;

    }

    String sfp = mCLI.getSourceFolderPath();
    String cfp = mCLI.getCandidateFolderPath();
    String key = mCLI.getComparisonKey();

    if ( !isValidInput( sfp, cfp ) ) {
      // TODO log
      return 3; // TODO proper exit code
    }
       
    // TODO iterate over source folder and compare file by file.
    return 0;
  }

  private boolean isValidInput( String sfp, String cfp ) {
    boolean valid = true;

    File sourceFolder = new File( sfp );
    File candidateFolder = new File( cfp );

    File[] sourceFiles = sourceFolder.listFiles( new FitsFileFilter() );
    File[] candidateFiles = candidateFolder.listFiles( new FitsFileFilter() );

    if ( sourceFiles == null || sourceFiles.length == 0 || candidateFiles == null || candidateFiles.length == 0 ) {
      valid = false;
    }

    return valid;
  }
  
  private void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "fbt", mCLI.getOptions() );
  }

  private class FitsFileFilter implements FileFilter {

    public boolean accept( File f ) {
      return f.getName().endsWith( "fits.xml" );
    }

  }

}
