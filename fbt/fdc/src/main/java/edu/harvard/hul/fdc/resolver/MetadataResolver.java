package edu.harvard.hul.fdc.resolver;

import java.util.List;

import org.dom4j.Element;

public class MetadataResolver extends DiffResolver {

  @Override
  protected void resolve( Element source, Element candidate ) {
    List<Element> sMetadata = source.elements();
    List<Element> cMetadata = candidate.elements();

    for (Element s : sMetadata) {
      String sMetadataType = s.getName();
      boolean found = false;
      for (Element c : cMetadata) {
        String cMetadataType = c.getName();
        if (sMetadataType.equals( cMetadataType )) {
          resolveMetadata( s, c );
          found = true;
          break;
        }
      }

      if (!found) {
        // TODO create a value mismatch
        // for all tools in the source tag.
      }

    }
  }

  private void resolveMetadata( Element source, Element candidate ) {

    List<Element> sources = source.elements();
    List<Element> candidates = candidate.elements();

    for (Element s : sources) {
      String sNodeName = s.getName();
      String sTool = s.attributeValue( "toolname" );
      String sValue = s.getText();

      boolean found = false;
      boolean equal = false;
      for (Element c : candidates) {
        String cNodeName = c.getName();
        String cTool = c.attributeValue( "toolname" );
        String cValue = c.getText();
        if (sNodeName.equals( cNodeName ) && sTool.equals( cTool )) {
          found = true;

          if (sValue.equals( cValue )) {
            equal = true;
            break;
          }

        }
      }

      if (found) {

        if (equal) {
          ToolGlobalMissingCounter counter = getCounter( sTool );
          counter.incrementSourceOccurs( 1 );
        } else {
          handleTool( sTool, mMismatchValues );
          System.out.println(mCurrentKey + ": " + s.asXML());
        }

      } else {
        missingTool( sTool );
      }
    }

  }

}
