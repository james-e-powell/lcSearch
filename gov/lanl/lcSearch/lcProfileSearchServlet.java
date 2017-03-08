package gov.lanl.lcSearch;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

import javax.servlet.*;
import javax.servlet.http.*;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import com.sun.org.apache.xerces.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

public class lcProfileSearchServlet extends HttpServlet {
  String queryString = "";
  String retType = "";
  String resultIdentifier = "";

  private static String graphName="";
  private static String serverAddr="";

  private static final String CONFIG_FILENAME = "si_server.properties";
  private static Properties config = new Properties();

  static {
        try {
            System.out.println("finding configuration file " + CONFIG_FILENAME);
            InputStream in = lcProfileSearchServlet.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME);
            config.load(in);

            graphName = config.getProperty("db_graph");
            serverAddr = config.getProperty("db_url");

            System.out.println("DB URL: " + serverAddr);
            System.out.println("DB Graph: " + graphName);
         } catch (IOException e) {
           throw new RuntimeException(e.getMessage());
         } // try
  }

 
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
      queryString = request.getParameter("query");
      retType = request.getParameter("return");
      doGet(request, response);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
      queryString = request.getParameter("query");
      retType = request.getParameter("return");

        response.setContentType("text/html");
        response.setHeader("Cache-Control", "no-cache");
      try { 
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Results</title></head><body>");
  
        out.println("<table>");

        if (queryString.indexOf(".")>-1) {
          resultIdentifier = queryString.substring(0, queryString.indexOf("."));
        } else {
          resultIdentifier = queryString;
        }
        String docMetadata = retrieveProfile(resultIdentifier);

        System.out.println(docMetadata);

        DOMParser parser = new DOMParser();
        // parser.parse(docMetadata);
        parser.parse(new InputSource(new StringReader(docMetadata)));
        Document doc = parser.getDocument();

        System.out.println("processing dom object");

        NodeList root = doc.getChildNodes();
        StringBuffer authorBuffer = new StringBuffer();
        StringBuffer ldrdBuffer = new StringBuffer();
        StringBuffer laurBuffer = new StringBuffer();
        StringBuffer metricsBuffer = new StringBuffer();
        String title="";
        String date = "";
        String identifier = "";
        String reportType = "";

        Node sparql=getNode("sparql", root);
        Node results=getNode("results", sparql.getChildNodes() );
        // Node result = getNode("result", results.getChildNodes() );
        NodeList resultChildren = results.getChildNodes();
        for (int index =1; index<resultChildren.getLength(); index++) {
          Node resultNode = resultChildren.item(index);
          System.out.println(resultNode.getNodeName());
          if (resultNode.getNodeName().equals("result")) {
          NodeList bindingList = resultNode.getChildNodes();
          StringBuffer aTitle = new StringBuffer();
          for (int bindex=1; bindex<bindingList.getLength(); bindex++) {
            authorBuffer = new StringBuffer();

            Node bindingNode = bindingList.item(bindex);
            System.out.println(bindingNode.getNodeName());
            if (bindingNode.getNodeName().equals("binding")) {
            String elementVal = "";
            String bindingAttr = getNodeAttr("name", bindingNode);
            System.out.println(bindingAttr);
            NodeList bindingChildren = bindingNode.getChildNodes();
            try {
              switch(bindingAttr) {
                case "title": 
                  System.out.println("processing title");
                  elementVal = getNodeValue(bindingChildren.item(1));
                  System.out.println("Title: " + elementVal);
                    title = elementVal;
                  break;
                case "znumber": 
                  System.out.println("processing znumber");
                  elementVal = getNodeValue(bindingChildren.item(1));
                  System.out.println("Z-number: " + elementVal);
                  if (index==1) {
                    elementVal = elementVal.replace("http://people.lanl.gov/", "");
                    out.println("<td><h3>z-number: " + elementVal + "</h3></td><td>&nbsp;</td></tr>");
                    out.println("<tr><td colspan=\"3\" align=\"center\" bgcolor=\"#E8D1D1\"><h2>Publications</h2></td></tr>");
                  
                  }
                  break;
                case "identifier": 
                  System.out.println("processing identifier");
                  elementVal = getNodeValue(bindingChildren.item(1));
                  System.out.println("Object Identifier: " + elementVal);
                  identifier = elementVal;
                  if (identifier.indexOf("ldrd")>-1) {
                    reportType = "ldrd";
                  } else {
                    if (identifier.indexOf("LA-UR")>-1) {
                      reportType = "laur";
                    } else { reportType = "metrics"; }
                  }
     
                  break;
                case "name": 
                  System.out.println("processing name");
                  elementVal = getNodeValue(bindingChildren.item(1));
                  System.out.println("Literal: " + elementVal);
                  if (index==1) {
                    out.println("<td><img src=\"/lcSearch/si_icon.png\"></td>");
                    out.println("<td colspan=\"2\"><h1>Profile for " + elementVal + "</h1></td></tr>");
                  }
                  break;
                case "date": 
                  System.out.println("processing date");
                  elementVal = getNodeValue(bindingChildren.item(1));
                  System.out.println("Literal: " + elementVal);
                  date = elementVal;
                  break;
                case "org":
                  System.out.println("processing org");
                  elementVal = getNodeValue(bindingChildren.item(1));
                  System.out.println("Literal: " + elementVal);
                  if (index==1) {
                    elementVal = elementVal.replace("http://lanl.gov/", "");
                    out.println("<tr><td><h2>Organization: " + elementVal + "</h2><td>");
                  }
                  break;
                default: break;
              }
            } catch (Exception e) { System.out.println("uri exception was " + e); }
          }
          }
          if (reportType=="ldrd") {
            ldrdBuffer.append("<dt><b>"+title + "</b>");
            ldrdBuffer.append("<dd>"+identifier + " <i>(" + date + ")</i> ");
          }
          if (reportType=="laur") {
            laurBuffer.append("<dt><b>"+title + "</b>");
            laurBuffer.append("<dd>"+identifier + " <i>(" + date + ")</i> ");
          }
          if (reportType=="metrics") {
            metricsBuffer.append("<dt><b>"+title + "</b>");
            metricsBuffer.append("<dd>"+identifier + " <i>(" + date + ")</i> ");
          }

        }

        }
        out.println("<tr><td valign=\"top\"><h2>LDRD</h2>");
        out.println(ldrdBuffer.toString());
        out.println("</td>");
        out.println("<td valign=\"top\"><h2>LA-UR</h2>");
        out.println(laurBuffer.toString());
        out.println("</td>");
        out.println("<td valign=\"top\"><h2>METRICS</h2>");
        out.println(metricsBuffer.toString());
        out.println("</td></tr>");
        out.println("</table>");
        out.println("</body></html>");
        out.close();
 
      } catch (Exception e) { System.out.println("servlet exception " + e); }
  }

  public String retrieveProfile(String identifier) {
    String charset = "UTF-8";
    String retValue = "";
    String query="";
    String nameParts[] = queryString.split(" ");

    try {
        // StringBuffer queryBuffer = new StringBuffer(" select * WHERE { {  ?identifier <http://purl.org/dc/elements/1.1/title> ?title.  ?identifier <http://purl.org/dc/elements/1.1/date> ?date. ?identifier <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?znumber.  optional { ?znumber <http://xmlns.com/foaf/0.1/name> ?name. ?znumber <http://www.w3.org/ns/org#memberOf> _:o.  _:o ?x ?org. ?identifier <http://purl.org/dc/elements/1.1/description> ?description. } }  filter (");
        StringBuffer queryBuffer = new StringBuffer(" select * WHERE { {  ?identifier <http://purl.org/dc/elements/1.1/title> ?title.  ?identifier <http://purl.org/dc/elements/1.1/date> ?date. ?identifier <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?znumber.  ?znumber <http://xmlns.com/foaf/0.1/name> ?name. ?znumber <http://www.w3.org/ns/org#memberOf> _:o.  _:o ?x ?org. ?identifier <http://purl.org/dc/elements/1.1/description> ?description. }   filter (");
        // StringBuffer queryBuffer = new StringBuffer(" select * WHERE { ?znumber <http://xmlns.com/foaf/0.1/name> ?name. ?znumber <http://www.w3.org/ns/org#memberOf> _:o.  _:o ?x ?org. }  filter (");

      for (String part : nameParts) {
        queryBuffer.append("regex(?name, \"" + part + "\", \"i\") && ");
      }
      queryBuffer.append(" }");
      query = queryBuffer.toString();
      query = query.replace(") &&  }", ")) }");

      System.out.println(query);
      String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
      URL url = new URL(serverAddr + "?query=" + encodedQuery);
      System.out.println(url.toString());
      URLConnection connection = url.openConnection();
      connection.setRequestProperty("Accept-Charset", charset);
      InputStream response = connection.getInputStream();
      String results = fromStream(response);
      System.out.println(results);
      StringBuffer resultSet = new StringBuffer();
      int i = results.indexOf("<literal>");
      while (i >= 0) {
        resultSet.append(results.substring(results.indexOf("<literal>", i), results.indexOf("</literal>", i+1)));
        i = results.indexOf("<literal>", i+1);
        resultSet.append("<p>");
      }

      retValue = results;
    } catch (Exception e) { System.out.println(e); }
    return retValue;
  }

  public static String fromStream(InputStream in) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    StringBuilder out = new StringBuilder();
    String newLine = System.getProperty("line.separator");
    String line;
    while ((line = reader.readLine()) != null) {
        out.append(line);
        out.append(newLine);
    }
    return out.toString();
  }

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

