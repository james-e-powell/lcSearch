package gov.lanl.lcSearch;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class rdfNetworkServlet extends HttpServlet {
  String queryString = "";
  String retType = "";
  String[] ids;
  HashMap<String,String> idLabelHash = new HashMap<String,String>();
  HashMap<String,node> nodes = new HashMap<String,node>();

  private static String graphName="";
  private static String serverAddr="";

  private static final String CONFIG_FILENAME = "si_server.properties";
  private static Properties config = new Properties();

  static {
        try {
            System.out.println("finding configuration file " + CONFIG_FILENAME);
            InputStream in = rdfQueryServlet.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME);
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
      retType = request.getParameter("return");
      ids = request.getParameterValues("ids");
      doGet(request, response);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
      retType = request.getParameter("return");
      ids = request.getParameterValues("ids");

      try { 
         PrintWriter out = response.getWriter();

         // response.setContentType("text/html");
         response.setContentType("application/json; charset=utf-8");
         response.setHeader("Cache-Control", "no-cache");

         HashMap<String,edge> docMetadata = resolveIdentifier(retType, ids);

         // out.println("{ \"Graph\": [\n");
         StringBuffer jsonBody = new StringBuffer();
         jsonBody.append("{ \"Graph\": {\n");
         jsonBody.append("   \"Edges\": [\n");
         for (String edgeKey : docMetadata.keySet()){
           jsonBody.append("  { \"Edge\": [\""+docMetadata.get(edgeKey).sourceId + "\",\"" + docMetadata.get(edgeKey).targetId + "\"]\n   },\n");
           System.out.println("obj title: " + docMetadata.get(edgeKey).sourceLabel);
           System.out.println("zname: " + docMetadata.get(edgeKey).targetLabel);
         }
         String edgesString = jsonBody.toString();
         edgesString = edgesString.substring(0, jsonBody.toString().length()-2);
         jsonBody = new StringBuffer(edgesString);

         jsonBody.append("], ");
         jsonBody.append("   \"Nodes\": [\n");
         // for (String nodeKey : idLabelHash.keySet()){
         //   jsonBody.append("  { \"Node\": [\"" + nodeKey + "\",\"" + idLabelHash.get(nodeKey) + "\"]\n  },\n");
         // }
         for (String nodeKey : nodes.keySet()) {
           if (nodeKey.indexOf("http://people.lanl.gov")>-1) {
             jsonBody.append("  { \"Node\": [\"" + nodeKey + "\",\"" + nodes.get(nodeKey).nodeLabel + "\",\"" + nodes.get(nodeKey).properties.get("org") + "\"]\n  },\n");
           } else {
             jsonBody.append("  { \"Node\": [\"" + nodeKey + "\",\"" + nodes.get(nodeKey).nodeLabel + "\",\"" + nodes.get(nodeKey).nodeDate + "\"]\n  },\n");
           }
         }
         String nodesString = jsonBody.toString();
         nodesString = nodesString.substring(0, jsonBody.toString().length()-2);
         jsonBody = new StringBuffer(nodesString);

         jsonBody.append("]");
         jsonBody.append(" }");
         jsonBody.append(" } } ");
         out.println(jsonBody.toString().substring(0, jsonBody.toString().length()-2));
         System.out.println(jsonBody.toString().substring(0, jsonBody.toString().length()-2));

         
         out.close();
 
      } catch (Exception e) { }
  }

  // public String resolveIdentifier(String retType, String[] ids) {
  public HashMap<String,edge> resolveIdentifier(String retType, String[] ids) {
    System.out.println("rdfNetwork");
    String charset = "UTF-8";
    // String graphName = "http://int.lanl.gov/SI";

    // Base address for server hosting a SPARQL query endpoint
    // mulgara server address 
    // String serverAddr = "http://localhost:8088/";
   
    // Allegrograph server address  
    // String serverAddr = "http://boots.lanl.gov:10035/repositories/SI";

    String retValue = "";
    String query="";
    String results="";
    StringBuffer resultSet = new StringBuffer();
    HashMap<String,edge> network = new HashMap<String,edge>();

    try {
      int count=0;
      for (String anId : ids) { 
        System.out.println(anId);
        anId = anId.substring(0, anId.indexOf("."));
        System.out.println(anId);
        // query = "select * WHERE { {  ?x <http://purl.org/dc/elements/1.1/title> ?y.  ?x <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?m. ?m <http://xmlns.com/foaf/0.1/name> ?t. } filter (str(?x)=\"" + anId + "\") }";
        // query = "select * WHERE { {  ?x <http://purl.org/dc/elements/1.1/title> ?y.  OPTIONAL { ?x <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?m. ?m <http://xmlns.com/foaf/0.1/name> ?t. } } filter (regex(str(?x), \"" + anId + "\")) }";
        anId = anId.replace("info-lanl-repo_","");
        query = "select * WHERE { {  ?identifier <http://purl.org/dc/elements/1.1/title> ?title.  ?identifier <http://purl.org/dc/elements/1.1/date> ?date. ?identifier <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?znumber. ?znumber <http://xmlns.com/foaf/0.1/name> ?name. ?znumber <http://www.w3.org/ns/org#memberOf> _:o.  _:o ?x ?org. } filter (regex(str(?identifier), \"" + anId + "\")) }";
        System.out.println(query);
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");

        // Constructs URL of SPARQL query for appropriate SPARQL query endpoint 
        // mulgara url
        // URL url = new URL(serverAddr + "sparql/?query=" + encodedQuery + "&default-graph-uri="+graphName);

        // Allegrograph url 
        URL url = new URL(serverAddr + "?query=" + encodedQuery);

        System.out.println(url.toString());

        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Accept-Charset", charset);
        InputStream response = connection.getInputStream();
        results = fromStream(response);
        System.out.println(results);
        resultSet.append(anId + "<p>");

        int titlePos = results.indexOf("binding name=\"title\">");
        String objTitle = "";
        if (titlePos>-1) {
           objTitle = results.substring(results.indexOf("<literal>", titlePos)+9, results.indexOf("</literal>", titlePos+1));
           objTitle = objTitle.replaceAll("\"", "");
        }

        int datePos = results.indexOf("binding name=\"date\">");
        String objDate = "";
        if (datePos>-1) {
           objDate = results.substring(results.indexOf("<literal>", datePos)+9, results.indexOf("</literal>", datePos+1));
           objDate = objDate.replaceAll("\"", "");
        }

        idLabelHash.put(anId, objTitle);
        node aNode = new node();
        aNode.nodeId=anId;
        aNode.nodeLabel=objTitle;
        aNode.nodeDate=objDate;
        nodes.put(anId, aNode);

        int i = results.indexOf("<result>"); 
        while (i >= 0) {
            try { 
              int namePos = results.indexOf("<binding name=\"name", i); 
              String zName = results.substring(results.indexOf("<literal>", namePos)+9, results.indexOf("</literal>", namePos+1));
              String aUri = "";
              String anOrg = "";

              int zPos = results.indexOf("binding name=\"znumber\">", i);
              if (zPos>-1) {
                 aUri = results.substring(results.indexOf("<uri>", zPos)+5, results.indexOf("</uri>", zPos+1));
                 // i = results.indexOf("<binding name=\"znumber", i+8);
              }

              int oPos = results.indexOf("binding name=\"org\">", i);
              if (oPos>-1) {
                 anOrg = results.substring(results.indexOf("<literal>", oPos)+9, results.indexOf("</literal>", oPos+1));
              }

              edge thisEdge = new edge();
              node thisNode = new node();

              thisNode.nodeId = aUri;
              thisNode.nodeLabel = zName;
              HashMap <String,String> nodeProperties = new HashMap<String,String>();
              nodeProperties.put("org", anOrg);
              thisNode.properties = nodeProperties;
              nodes.put(aUri, thisNode);

              thisEdge.sourceId=anId;
              thisEdge.targetId=aUri;
              thisEdge.sourceLabel=objTitle;
              thisEdge.targetLabel=zName;

              idLabelHash.put(aUri,zName);

              network.put(Integer.toString(count), thisEdge);
              count++;
              // network.put(aUri,anId);
              resultSet.append("<p>");
              i = results.indexOf("<result>", zPos);
            } catch (Exception e) { System.out.println("exception was " + e); }
        }
        retValue = resultSet.toString();
      }
    } catch (Exception e) { System.out.println(e); }
    retValue = resultSet.toString();
    
    // return retValue;
    return network;
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

}
