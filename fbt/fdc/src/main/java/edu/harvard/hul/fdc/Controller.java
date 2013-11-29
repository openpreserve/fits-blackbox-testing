package edu.harvard.hul.fdc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import edu.harvard.hul.fdc.cli.CLI;

public class Controller {

  private CLI mCLI;

  private ControllerState mState;

  private FitsXMLComparator mComparator;

  private LogWriter mLogger;

  private String[] mInput;

  public Controller( CLI cli, ControllerState state, FitsXMLComparator comp, LogWriter log ) {
    mCLI = cli;
    mState = state;
    mComparator = comp;
    mLogger = log;
  }

  public void setInput( String... args ) {
    mInput = args;
  }

  public void run() {

    if (!isValidInput()) {
      // TODO log
      printHelp();
      return;
    }

    String sfp = mCLI.getSourceFolderPath();
    String cfp = mCLI.getCandidateFolderPath();
    String key = mCLI.getComparisonKey();

    traverseFiles( sfp, cfp );
    mLogger.flush( key );
  }

  public ControllerState getState() {
    return mState;
  }

  private boolean isValidInput() {

    if (mInput == null) {
      handleState( ControllerState.SYSTEM_ERROR );
      return false;
    }

    try {

      mCLI.parse( mInput );

    } catch (ParseException e) {

      if (e.getMessage().equals( "HELP" )) {
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

    if (sourceFiles == null || sourceFiles.length == 0 || candidateFiles == null || candidateFiles.length == 0) {
      valid = false;
      handleState( ControllerState.TEST_NOT_EXECUTABLE );
    }

    return valid;
  }

  private void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "fbt", mCLI.getOptions() );
  }

  private void handleState( int state ) {
    mState.assignState( state );
  }

  private void handleSummary( List<Report> reports ) {
    for (Report r : reports) {
      mLogger.submitLog( r.getLog() );
      handleState( r.getStatus() );
    }
  }

  private void traverseFiles( String sourceFolderPath, String candidateFolderPath ) {
    FitsFileFilter filter = new FitsFileFilter();
    File sourceFolder = new File( sourceFolderPath );
    File candidateFolder = new File( candidateFolderPath );
    List<File> sourceFiles = new ArrayList<File>( Arrays.asList( sourceFolder.listFiles( filter ) ) );
    List<File> candidateFiles = new ArrayList<File>( Arrays.asList( candidateFolder.listFiles( filter ) ) );

    Iterator<File> iter = sourceFiles.iterator();
    while (iter.hasNext()) {
      File sf = iter.next();
      File cf = new File( candidateFolder, sf.getName() );

      if (candidateFiles.contains( cf )) {
        try {
          String sXML = IOUtils.toString( new FileInputStream( sf ) );
          String cXML = IOUtils.toString( new FileInputStream( cf ) );

          mComparator.compareWithDom4J( cf.getName(), sXML, cXML );

        } catch (IOException e) {
          e.printStackTrace();
          handleState( ControllerState.SYSTEM_ERROR );
        }

        candidateFiles.remove( cf );
      } else {
        if (!isSystemFile( cf.getName() )) {
          mLogger.submitLog( "Missing candidate file: " + cf.getName() );
          handleState( ControllerState.FILE_MISSING_CANDIDATE );
        }

      }

      iter.remove();

    }

    if (candidateFiles.size() > 0) {
      for (File f : candidateFiles) {
        if (!isSystemFile( f.getName() )) {
          mLogger.submitLog( "Missing source file: " + f.getName() );
          handleState( ControllerState.FILE_MISSING_SOURCE );
        }
      }
    }

    List<Report> summary = mComparator.getComparisonSummary();
    handleSummary( summary );

  }

  private class FitsFileFilter implements FilenameFilter {

    public boolean accept( File dir, String name ) {
      return name.endsWith( "fits.xml" );
    }

  }

  private boolean isSystemFile( String name ) {
    boolean systemFile = false;
    if (name.startsWith( ".DS_Store" )) {
      systemFile = true;
    }

    return systemFile;
  }

}
