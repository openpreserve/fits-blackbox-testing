package edu.harvard.hul.fdc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import edu.harvard.hul.fdc.resolver.DiffResolver;
import edu.harvard.hul.fdc.resolver.FileInfoResolver;
import edu.harvard.hul.fdc.resolver.IdentificationResolver;
import edu.harvard.hul.fdc.resolver.MetadataResolver;

public class FitsXMLComparator {

  private Map<String, DiffResolver> mResolvers;

  public FitsXMLComparator() {
    mResolvers = new HashMap<String, DiffResolver>();
    mResolvers.put( "identification", new IdentificationResolver() );
    mResolvers.put( "fileinfo", new FileInfoResolver() );
    mResolvers.put( "metadata", new MetadataResolver() );
  }

  public void compareWithDom4J( String fileName, String source, String candidate ) {
    try {
      Document sDoc = DocumentHelper.parseText( source );
      Document cDoc = DocumentHelper.parseText( candidate );

      Element element = sDoc.getRootElement();
      List<Element> elements = element.elements();
      for (Element e : elements) {
        String nodeName = e.getName();
        if (nodeName != null) {
          DiffResolver diffResolver = mResolvers.get( nodeName );
          if (diffResolver != null) {
            String xPath = e.getPath();
            Element candidateNode = (Element) cDoc.selectSingleNode( xPath );
            diffResolver.resolve( fileName, e, candidateNode );
          }

        }
      }

    } catch (DocumentException e) {
      e.printStackTrace();
    }
  }

  public List<Report> getComparisonSummary() {
    DiffResolver tmp = new DiffResolver() {
      @Override
      public void resolve( Element source, Element candidate ) {
      }
    };

    for (String k : mResolvers.keySet()) {
      DiffResolver resolver = mResolvers.get( k );
      tmp.merge( resolver );
    }

    return tmp.report();
  }

}
