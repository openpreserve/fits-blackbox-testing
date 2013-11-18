package edu.harvard.hul.fbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import edu.harvard.hul.fbt.cli.CLI;

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

  private void handleAnomalies( List<Anomaly> anomalies ) {
    List<Anomaly> missingTools = getAnomaliesByType( anomalies, Anomaly.MISSING_TOOL );

    FitsFileFilter filter = new FitsFileFilter();
    List<File> candidateNames = new ArrayList<File>( Arrays.asList( new File( mCLI.getCandidateFolderPath() )
        .listFiles( filter ) ) );

    Map<String, List<String>> aggregatedTools = new HashMap<String, List<String>>();

    for (Anomaly a : missingTools) {
      mLogger.submitLog( a.getFileName(), String.format( "Missing tool: [%s]", a.getData() ) );

      List<String> list = aggregatedTools.get( a.getData().toString() );

      if (list == null) {
        list = new ArrayList<String>();
        list.add( a.getFileName() );
        aggregatedTools.put( a.getData().toString(), list );
      } else {
        list.add( a.getFileName() );
      }
    }

    Iterator<File> candidateFiles = candidateNames.iterator();
    Map<String, Boolean> globalMissingTools = new HashMap<String, Boolean>();
    while (candidateFiles.hasNext()) {
      File file = candidateFiles.next();
      try {

        String cXML = IOUtils.toString( new FileInputStream( file ) );

        for (String tool : aggregatedTools.keySet()) {

          Boolean global = globalMissingTools.get( tool );
          if (global == null) {
            globalMissingTools.put( tool, true );
            break;
          }

          if (global == true && cXML.contains( String.format( "toolname=\"%s\"", tool ) )) {
            globalMissingTools.put( tool, false );
          }

        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    for (String tool : globalMissingTools.keySet()) {
      if (globalMissingTools.get( tool )) {
        mLogger.submitGlobalLog( String.format( "[%s] is missing from all candidate files, where it was also present in the source file", tool ) );
        handleState( ControllerState.TOOL_MISSING_OUTPUT );

      }
    }

  }

  private List<Anomaly> getAnomaliesByType( List<Anomaly> anomalies, String type ) {
    List<Anomaly> result = new ArrayList<Anomaly>();

    if (anomalies != null) {
      for (Anomaly a : anomalies) {
        if (a.getType().equals( type )) {
          result.add( a );
        }
      }
    }

    return result;
  }

  private void traverseFiles( String sourceFolderPath, String candidateFolderPath ) {
    FitsFileFilter filter = new FitsFileFilter();
    File sourceFolder = new File( sourceFolderPath );
    File candidateFolder = new File( candidateFolderPath );
    List<File> sourceFiles = new ArrayList<File>( Arrays.asList( sourceFolder.listFiles( filter ) ) );
    List<File> candidateFiles = new ArrayList<File>( Arrays.asList( candidateFolder.listFiles( filter ) ) );
    List<Anomaly> anomalies = new ArrayList<Anomaly>();

    Iterator<File> iter = sourceFiles.iterator();
    while (iter.hasNext()) {
      File sf = iter.next();
      File cf = new File( candidateFolder, sf.getName() );

      if (candidateFiles.contains( cf )) {
        try {
          String sXML = IOUtils.toString( new FileInputStream( sf ) );
          String cXML = IOUtils.toString( new FileInputStream( cf ) );
          // System.out.println( "Comparing: " + sf.getName() );

          anomalies.addAll( mComparator.compare( cf.getName(), sXML, cXML ) );

        } catch (IOException e) {
          e.printStackTrace();
          handleState( ControllerState.SYSTEM_ERROR );
        }

        // TODO handle state
        // TODO log output

        candidateFiles.remove( cf );
      } else {

        Anomaly a = new Anomaly( Anomaly.MISSING_CANDIDATE, cf.getName() );
        anomalies.add( a );

      }

      iter.remove();

    }

    if (candidateFiles.size() > 0) {
      for (File f : candidateFiles) {
        Anomaly a = new Anomaly( Anomaly.MISSING_SOURCE, f.getName() );
        anomalies.add( a );
      }
    }

    handleAnomalies( anomalies );

  }

  private class FitsFileFilter implements FilenameFilter {

    public boolean accept( File dir, String name ) {
      return name.endsWith( "fits.xml" );
    }

  }

}
