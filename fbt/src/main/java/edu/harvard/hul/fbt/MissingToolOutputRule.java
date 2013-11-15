package edu.harvard.hul.fbt;

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

  public void checkDifferences(List<Difference> differences) {
    for (Difference d : differences) {
      if (d.getId() == DifferenceConstants.CHILD_NODE_NOT_FOUND_ID) {
        NodeDetail controlNodeDetail = d.getControlNodeDetail();
        Node node = controlNodeDetail.getNode();

        if (node != null) {
          NamedNodeMap attributes = node.getAttributes();
          if (attributes != null) {
            Node toolname = attributes.getNamedItem("toolname");
            if (toolname != null) {
              mMissingTools.add(toolname.getNodeValue());
            }
          }
        }
      }
    }
  }

  public boolean hasMissing() {
    return mMissingTools.size() > 0;
  }

  public Set<String> getMissingTools() {
    return mMissingTools;
  }

}
