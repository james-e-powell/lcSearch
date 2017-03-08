import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
 
public class parseSparql {

protected static Node getNode(String tagName, NodeList nodes) {
    for ( int x = 0; x < nodes.getLength(); x++ ) {
        Node node = nodes.item(x);
        if (node.getNodeName().equalsIgnoreCase(tagName)) {
            return node;
        }
    }
 
    return null;
}
 
protected static String getNodeValue( Node node ) {
    NodeList childNodes = node.getChildNodes();
    for (int x = 0; x < childNodes.getLength(); x++ ) {
        Node data = childNodes.item(x);
        if ( data.getNodeType() == Node.TEXT_NODE )
            return data.getNodeValue();
    }
    return "";
}
 
protected static String getNodeValue(String tagName, NodeList nodes ) {
    for ( int x = 0; x < nodes.getLength(); x++ ) {
        Node node = nodes.item(x);
        if (node.getNodeName().equalsIgnoreCase(tagName)) {
            NodeList childNodes = node.getChildNodes();
            for (int y = 0; y < childNodes.getLength(); y++ ) {
                Node data = childNodes.item(y);
                if ( data.getNodeType() == Node.TEXT_NODE )
                    return data.getNodeValue();
            }
        }
    }
    return "";
}
 
protected static String getNodeAttr(String attrName, Node node ) {
    NamedNodeMap attrs = node.getAttributes();
    for (int y = 0; y < attrs.getLength(); y++ ) {
        Node attr = attrs.item(y);
        if (attr.getNodeName().equalsIgnoreCase(attrName)) {
            return attr.getNodeValue();
        }
    }
    return "";
}
 
protected static String getNodeAttr(String tagName, String attrName, NodeList nodes ) {
    for ( int x = 0; x < nodes.getLength(); x++ ) {
        Node node = nodes.item(x);
        if (node.getNodeName().equalsIgnoreCase(tagName)) {
            NodeList childNodes = node.getChildNodes();
            for (int y = 0; y < childNodes.getLength(); y++ ) {
                Node data = childNodes.item(y);
                if ( data.getNodeType() == Node.ATTRIBUTE_NODE ) {
                    if ( data.getNodeName().equalsIgnoreCase(attrName) )
                        return data.getNodeValue();
                }
            }
        }
    }
 
    return "";
}


  public static void main (String args[]) {
    try {
        DOMParser parser = new DOMParser();
        parser.parse("sparql2.xml");
        Document doc = parser.getDocument();

        NodeList root = doc.getChildNodes();

        Node sparql=getNode("sparql", root);
        Node results=getNode("results", sparql.getChildNodes() );
        Node result = getNode("result", results.getChildNodes() );
        NodeList resultSet = results.getChildNodes();
        for (int rindex =1; rindex<resultSet.getLength(); rindex++) {

        NodeList bindings = resultSet.item(rindex).getChildNodes();
        for (int bindex = 1; bindex<bindings.getLength(); bindex++) {
          NodeList resultChildren = bindings.item(bindex).getChildNodes();
          for (int index=1; index<resultChildren.getLength(); index++) {
            try { 
              String nodeName = resultChildren.item(index).getNodeName();
              if (nodeName.equals("uri")) {
                String uri = getNodeValue(resultChildren.item(index));
                System.out.println("Uri: " + uri);
              }
              if (nodeName.equals("literal")) {
                String literal = getNodeValue(resultChildren.item(index));
                System.out.println("Literal: " + literal);
              }
            } catch (Exception e) { System.out.println("uri exception was " + e); }
          }
        }}
 
    } catch ( Exception e ) {
        e.printStackTrace();
    }
  }

}

