package edu.harvard.hul.fdc.resolver;

import java.util.List;

import org.dom4j.Element;

public class FileInfoResolver extends DiffResolver {
  
  @Override
  public void resolve( Element source, Element candidate ) {
    List<Element> sources = source.elements();
    List<Element> candidates = candidate.elements();

    for (Element s : sources) {
      String sNodeName = s.getName();
      String sTool = s.attributeValue( "toolname" );
      boolean found = false;
      for (Element c : candidates) {
        String cNodeName = c.getName();
        String cTool = c.attributeValue( "toolname" );

        if (sNodeName.equals( cNodeName ) && sTool.equals( cTool )) {
          found = true;
          // found: compare values
          
          if (!s.getText().equals( c.getText() )) {
            handleTool( sTool, mMismatchValues );
          }
          
          break;
        }
      }

      if (!found) {

        missingTool( sTool );

      } else {
        ToolGlobalMissingCounter counter = getCounter( sTool );
        counter.incrementSourceOccurs( 1 );
      }
    }
  }

}
