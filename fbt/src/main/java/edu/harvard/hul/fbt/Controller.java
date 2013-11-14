package edu.harvard.hul.fbt;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import edu.harvard.hul.fbt.cli.CLI;

public class Controller {

  private CLI mCLI;

  private ControllerState mState;

  private String[] mInput;

  public Controller(CLI cli, ControllerState state) {
    mCLI = cli;
    mState = state;
  }

  public void setInput( String... args ) {
    mInput = args;
  }

  public void run() {

    if ( !isValidInput() ) {
      // TODO log
      printHelp();
      return;
    }

    String sfp = mCLI.getSourceFolderPath();
    String cfp = mCLI.getCandidateFolderPath();
    String key = mCLI.getComparisonKey();

    // TODO iterate over source folder and compare file by file.
  }

  public ControllerState getState() {
    return mState;
  }

  private boolean isValidInput() {

    if ( mInput == null ) {
      handleState( ControllerState.SYSTEM_ERROR );
      return false;
    }

    try {

      mCLI.parse( mInput );

    } catch ( ParseException e ) {

      if ( e.getMessage().equals( "HELP" ) ) {
        handleState( ControllerState.OK );
      } else {
        handleState( ControllerState.SYSTEM_ERROR );
      }

      return false;
    }

    boolean valid = true;
    File sourceFolder = new File( mCLI.getSourceFolderPath() );
    File candidateFolder = new File( mCLI.getCandidateFolderPath() );

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

  private void handleState( int state ) {
    mState.assignState( state );
    // TODO do also logging...
  }

  private class FitsFileFilter implements FileFilter {

    public boolean accept( File f ) {
      return f.getName().endsWith( "fits.xml" );
    }

  }

}
