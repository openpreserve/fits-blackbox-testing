package edu.harvard.hul.fbt;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import edu.harvard.hul.fbt.cli.CLI;

public class Controller {

  private CLI mCLI;

  private ControllerState mState;

  private FitsXMLComparator mComparator;

  private String[] mInput;

  public Controller(CLI cli, ControllerState state, FitsXMLComparator comp) {
    mCLI = cli;
    mState = state;
    mComparator = comp;
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

    traverseFiles( sfp, cfp );
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
  
  private void handleComparisonResult(String comparisonKey, ComparisonResult result) {
    Set<Integer> statusCodes = result.getStatusCodes();
    List<String> logs = result.getLogs();
    for (int sc : statusCodes) {
      mState.assignState(sc);
    }
    
    for (String l : logs) {
      //TODO write logs.
      // the log writer should cache the logs and even compress them.
      // they should be written upon flush at the end. 
      System.out.println(comparisonKey + ": " + l);
    }
  }

  private void traverseFiles( String sourceFolderPath, String candidateFolderPath ) {
    FitsFileFilter filter = new FitsFileFilter();
    File sourceFolder = new File( sourceFolderPath );
    File candidateFolder = new File( candidateFolderPath );
    List<File> sourceFiles = new ArrayList<File>( Arrays.asList( sourceFolder.listFiles( filter ) ) );
    List<File> candidateFiles = new ArrayList<File>( Arrays.asList( candidateFolder.listFiles( filter ) ) );

    Iterator<File> iter = sourceFiles.iterator();
    while ( iter.hasNext() ) {
      File sf = iter.next();
      File cf = new File( candidateFolder, sf.getName() );

      if ( candidateFiles.contains( cf ) ) {
        try {
          String sXML = IOUtils.toString( new FileInputStream( sf ) );
          String cXML = IOUtils.toString( new FileInputStream( cf ) );
          //System.out.println( "Comparing: " + sf.getName() );
          ComparisonResult result = mComparator.compare( sXML, cXML );
          handleComparisonResult(sf.getName(), result);

        } catch ( IOException e ) {
          e.printStackTrace();
          handleState( ControllerState.SYSTEM_ERROR );
        }
        
        // TODO handle state
        // TODO log output

        candidateFiles.remove( cf );
      } else {
        handleState( ControllerState.FILE_MISSING_CANDIDATE );

      }

      iter.remove();

    }

    if ( candidateFiles.size() > 0 ) {
      handleState( ControllerState.FILE_MISSING_SOURCE );
      // TODO iterate and log
    }
  }

  private class FitsFileFilter implements FileFilter {

    public boolean accept( File f ) {
      return f.getName().endsWith( "fits.xml" );
    }

  }

}
