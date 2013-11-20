package edu.harvard.hul.fdc.resolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

public class IdentificationResolver extends DiffResolver {

  @Override
  public void resolve( Element source, Element candidate ) {
    List<Element> sIdentities = source.elements( "identity" );
    List<Element> cIdentities = candidate.elements();

    for (Element sI : sIdentities) {
      findIdentityForResolution( sI, cIdentities );
    }

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
        counter.incrementSourceOccurs(1);
      }
    }
  }

}
