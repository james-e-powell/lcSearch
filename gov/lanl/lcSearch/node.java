package gov.lanl.lcSearch;

import java.util.HashMap;

public class node {
  String nodeId;
  String nodeLabel;
  String nodeDate;
  HashMap<String,String> properties = new HashMap<String,String>();

  public void setNodeId(String nodeIdParam) {
    nodeId = nodeIdParam;
  }

}
