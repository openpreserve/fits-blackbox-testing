package edu.harvard.hul.fbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

public class FitsXMLComparator {

  private Map<String, DiffResolver> mResolvers;

  public FitsXMLComparator() {
    mResolvers = new HashMap<String, DiffResolver>();
    mResolvers.put( "identification", new IdentificationResolver() );
  }

  public void compareWithDom4J( String fileName, String source, String candidate ) {
    try {
      Document sDoc = DocumentHelper.parseText( source );
      Document cDoc = DocumentHelper.parseText( candidate );

      treeWalk( sDoc, new NodeFoundCallback( fileName, cDoc ) );

    } catch (DocumentException e) {
      e.printStackTrace();
    }
  }

  public List<Report> getComparisonSummary() {
    List<Report> reports = new ArrayList<Report>();
    for (String k : mResolvers.keySet()) {
      DiffResolver resolver = mResolvers.get( k );
      reports.addAll( resolver.report() );
    }

    return reports;
  }

  private void treeWalk( Document document, NodeFoundCallback callback ) {
    Element element = document.getRootElement();
    List<Element> elements = element.elements();
    for (Element e : elements) {
      callback.callback( e );
    }
  }

  private class NodeFoundCallback {

    private String mFileName;

    private Document mCanditate;

    public NodeFoundCallback( String fileName, Document candidate ) {
      mFileName = fileName;
      mCanditate = candidate;
    }

    public void callback( Element node ) {
      String nodeName = node.getName();
      if (nodeName != null) {
        DiffResolver diffResolver = mResolvers.get( nodeName );
        if (diffResolver != null) {
          Element candidate = findCandidate( node );
          diffResolver.resolve( mFileName, node, candidate );
        }

      }
    }

    private Element findCandidate( Element source ) {
      String xPath = source.getPath();
      Node candidateNode = mCanditate.selectSingleNode( xPath );
      // System.out.println( "CANDIDATE" );
      // System.out.println( candidateNode.asXML() );
      return (Element) candidateNode;
    }
  }

}
