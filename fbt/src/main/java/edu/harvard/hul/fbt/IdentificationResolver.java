package edu.harvard.hul.fbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

public class IdentificationResolver extends DiffResolver {

  private String mCurrentKey;

  private Map<String, Set<String>> mUpdatedTools;

  private Map<String, Set<String>> mNewTools;

  private Map<String, Set<String>> mMissingTools;

  private Map<String, ToolGlobalMissingCounter> mGlobalMisses;

  public IdentificationResolver() {
    mUpdatedTools = new HashMap<String, Set<String>>();
    mNewTools = new HashMap<String, Set<String>>();
    mMissingTools = new HashMap<String, Set<String>>();
    mGlobalMisses = new HashMap<String, IdentificationResolver.ToolGlobalMissingCounter>();
  }

  @Override
  public void resolve( String fileName, Element source, Element candidate ) {
    mCurrentKey = fileName;
    // System.out.println( "RESOLVE" );
    List<Element> sIdentities = source.elements( "identity" );
    List<Element> cIdentities = candidate.elements();

    for (Element sI : sIdentities) {
      findIdentityForResolution( sI, cIdentities );
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

  private void print( Map<String, Set<String>> tools ) {
    for (String t : tools.keySet()) {
      System.out.println( t );
      Set<String> set = tools.get( t );
      for (String s : set) {
        System.out.println( "\t" + s );
      }
    }
  }

  private void findIdentityForResolution( Element sIdentity, List<Element> candidateIdentities ) {
    boolean found = false;

    if (candidateIdentities != null && candidateIdentities.size() > 0) {
      for (Element cIdentity : candidateIdentities) {
        String sFormat = sIdentity.attributeValue( "format" );
        String cFormat = cIdentity.attributeValue( "format" );
        String sMime = sIdentity.attributeValue( "mimetype" );
        String cMime = cIdentity.attributeValue( "mimetype" );
        String sTool = sIdentity.attributeValue( "toolname" );
        String cTool = cIdentity.attributeValue( "toolname" );

        if (sFormat.equals( cFormat ) && sMime.equals( cMime ) && sTool.equals( cTool )) {
          found = true;
          resolveIdentities( sIdentity, cIdentity );
          break;
        }
      }
    }

    if (!found) {
      List<Element> elements = sIdentity.elements( "tool" );
      for (Element e : elements) {
        String tool = e.attributeValue( "toolname" );
        missingTool( tool );
      }

    }
  }

  private void resolveIdentities( Element source, Element candidate ) {
    List<Element> elements = source.elements( "tool" );
    Set<String> tools = new HashSet<String>();
    Map<String, String> toolVersions = new HashMap<String, String>();
    for (Element e : elements) {
      String tool = e.attributeValue( "toolname" );
      tools.add( tool );
      toolVersions.put( tool, e.attributeValue( "toolversion" ) );
    }

    List<Element> candidates = candidate.elements( "tool" );
    Set<String> cTools = new HashSet<String>();

    for (Element c : candidates) {
      String cTool = c.attributeValue( "toolname" );
      cTools.add( cTool );

      String version = toolVersions.get( cTool );
      if (version == null) {

        handleTool( cTool, mNewTools );

      } else {
        String cVersion = c.attributeValue( "toolversion" );
        if (!version.equals( cVersion )) {
          handleTool( cTool, mUpdatedTools );
        }
      }
    }

    for (String t : tools) {
      if (!cTools.contains( t )) {
        missingTool( t );
      } else {
        ToolGlobalMissingCounter counter = getCounter( t );
        counter.incrementSourceOccurs();
      }
    }
  }

  private void missingTool( String tool ) {
    handleTool( tool, mMissingTools );
    ToolGlobalMissingCounter counter = getCounter( tool );
    counter.incrementSourceOccurs();
    counter.incrementCandidateMiss();
  }

  private void handleTool( String tool, Map<String, Set<String>> toolsSet ) {
    Set<String> files = getFiles( tool, toolsSet );
    files.add( mCurrentKey );
  }

  private Set<String> getFiles( String tool, Map<String, Set<String>> toolsSet ) {
    Set<String> set = toolsSet.get( tool );
    if (set == null) {
      set = new HashSet<String>();
      toolsSet.put( tool, set );
    }

    return set;
  }

  private ToolGlobalMissingCounter getCounter( String tool ) {
    ToolGlobalMissingCounter counter = mGlobalMisses.get( tool );
    if (counter == null) {
      counter = new ToolGlobalMissingCounter( tool );
      mGlobalMisses.put( tool, counter );
    }

    return counter;
  }

  private class ToolGlobalMissingCounter {
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

    public void incrementSourceOccurs() {
      mSourceOcurrs += 1;
    }

    public void incrementCandidateMiss() {
      mCandidateMiss += 1;
    }
  }

}
