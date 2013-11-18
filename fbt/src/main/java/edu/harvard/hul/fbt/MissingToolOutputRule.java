package edu.harvard.hul.fbt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.NodeDetail;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MissingToolOutputRule {

  private Set<String> mMissingTools;

  public MissingToolOutputRule() {
    mMissingTools = new HashSet<String>();
  }

  public void checkDifferences( List<Difference> differences ) {
    for (Difference d : differences) {
      if (d.getId() == DifferenceConstants.CHILD_NODE_NOT_FOUND_ID) {
        NodeDetail controlNodeDetail = d.getControlNodeDetail();
        Node node = controlNodeDetail.getNode();

        handleNode( node );
      }
    }
  }

  public boolean hasMissing() {
    return mMissingTools.size() > 0;
  }

  public Set<String> getMissingTools() {
    return mMissingTools;
  }

  public ComparisonResult getComparisonResult() {
    int statusCode;
    List<String> logs = new ArrayList<String>();
    if (hasMissing()) {
      statusCode = ControllerState.TOOL_MISSING_OUTPUT;
      for (String t : getMissingTools()) {
        logs.add( String.format( "Missing tool: ['%s']", t ) );
      }
    } else {
      statusCode = ControllerState.OK;
    }

    return new ComparisonResult( statusCode, logs );
  }

  private void handleNode( Node node ) {
    if (node != null) {
      if (node.getNodeName().equals( "externalIdentifier" )) {
        return;
      }
      NamedNodeMap attributes = node.getAttributes();

      if (attributes != null) {
        Node toolname = attributes.getNamedItem( "toolname" );
        if (toolname != null) {
          String tool = toolname.getNodeValue();
          if (tool != null) {
            if (tool.equals( "FITS" )) {
              Node child = node.getFirstChild();
              handleNode( child );
            } else {
              mMissingTools.add( toolname.getNodeValue() );
            }
          }

        }
      }
    }
  }
}
