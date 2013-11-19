package edu.harvard.hul.fbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

public abstract class DiffResolver {

  protected String mCurrentKey;

  protected Map<String, Set<String>> mUpdatedTools;

  protected Map<String, Set<String>> mNewTools;

  protected Map<String, Set<String>> mMissingTools;

  protected Map<String, ToolGlobalMissingCounter> mGlobalMisses;

  public DiffResolver() {
    mUpdatedTools = new HashMap<String, Set<String>>();
    mNewTools = new HashMap<String, Set<String>>();
    mMissingTools = new HashMap<String, Set<String>>();
    mGlobalMisses = new HashMap<String, ToolGlobalMissingCounter>();
  }

  public abstract void resolve( String fileName, Element source, Element candidate );

  public void merge( DiffResolver resolver ) {
    mergeToolsMap( mUpdatedTools, resolver.mUpdatedTools );
    mergeToolsMap( mNewTools, resolver.mNewTools );
    mergeToolsMap( mMissingTools, resolver.mMissingTools );
    
    for (String k : resolver.mGlobalMisses.keySet()) {
      ToolGlobalMissingCounter counter = mGlobalMisses.get( k );
      if (counter != null) {
        counter.incrementSourceOccurs( resolver.mGlobalMisses.get( k ).getSourceOccurs());
        counter.incrementCandidateMiss( resolver.mGlobalMisses.get( k ).getCandidateMiss());
      } else {
        counter = resolver.mGlobalMisses.get( k );
      }
      
      mGlobalMisses.put( k, counter );
    }
  }

  private void mergeToolsMap( Map<String, Set<String>> thisMap, Map<String, Set<String>> resolverMap ) {
    for (String k : resolverMap.keySet()) {
      Set<String> set = thisMap.get( k );
      if (set != null) {
        set.addAll( resolverMap.get( k ) );
      } else {
        set = resolverMap.get( k );
      }

      thisMap.put( k, set );
    }
  }

  public List<Report> report() {
    List<Report> reports = new ArrayList<Report>();
    Report report = generateReport( String.format( "Found %s new tool(s):\n", mNewTools.keySet().size() ), mNewTools );
    addReport( reports, report );

    report = generateReport( String.format( "Found %s updated tool(s):\n", mUpdatedTools.keySet().size() ),
        mUpdatedTools );
    addReport( reports, report );

    report = generateReport( String.format( "Found %s missing tool(s):\n", mMissingTools.keySet().size() ),
        mMissingTools );
    addReport( reports, report );

    if (mGlobalMisses.keySet().size() > 0) {
      report = new Report();
      int global = 0;
      String log = "Found %s tool(s) missing in all candidate files:\n";
      for (String t : mGlobalMisses.keySet()) {
        ToolGlobalMissingCounter counter = mGlobalMisses.get( t );
        if (counter.getSourceOccurs() == counter.getCandidateMiss()) {
          global += 1;
          log += t + "\n";
          report.setStatus( ControllerState.TOOL_MISSING_OUTPUT );
        }
      }

      log = String.format( log, global );
      report.setLog( log );
      addReport( reports, report );
    }

    return reports;
  }

  private void addReport( List<Report> list, Report r ) {
    if (r != null) {
      list.add( r );
    }
  }

  private Report generateReport( String lead, Map<String, Set<String>> toolsSet ) {
    Report newTools = null;
    if (toolsSet.keySet().size() > 0) {
      newTools = new Report();
      String log = lead;
      for (String t : toolsSet.keySet()) {
        log += t + "\n";
        Set<String> set = toolsSet.get( t );
        for (String f : set) {
          log += "\t" + f + "\n";
        }
      }
      newTools.setLog( log );
      newTools.setStatus( ControllerState.OK ); // should this be always ok?
    }

    return newTools;
  }

  protected void missingTool( String tool ) {
    handleTool( tool, mMissingTools );
    ToolGlobalMissingCounter counter = getCounter( tool );
    counter.incrementSourceOccurs(1);
    counter.incrementCandidateMiss(1);
  }

  protected void handleTool( String tool, Map<String, Set<String>> toolsSet ) {
    Set<String> files = getFiles( tool, toolsSet );
    files.add( mCurrentKey );
  }

  protected Set<String> getFiles( String tool, Map<String, Set<String>> toolsSet ) {
    Set<String> set = toolsSet.get( tool );
    if (set == null) {
      set = new HashSet<String>();
      toolsSet.put( tool, set );
    }

    return set;
  }

  protected ToolGlobalMissingCounter getCounter( String tool ) {
    ToolGlobalMissingCounter counter = mGlobalMisses.get( tool );
    if (counter == null) {
      counter = new ToolGlobalMissingCounter( tool );
      mGlobalMisses.put( tool, counter );
    }

    return counter;
  }

  protected class ToolGlobalMissingCounter {
    private String mTool;

    private int mSourceOcurrs;

    private int mCandidateMiss;

    public ToolGlobalMissingCounter( String tool ) {
      mTool = tool;
      mSourceOcurrs = 0;
      mCandidateMiss = 0;
    }

    public int getSourceOccurs() {
      return mSourceOcurrs;
    }

    public int getCandidateMiss() {
      return mCandidateMiss;
    }

    public void incrementSourceOccurs(int inc) {
      mSourceOcurrs += inc;
    }

    public void incrementCandidateMiss(int inc) {
      mCandidateMiss += inc;
    }
  }
}
