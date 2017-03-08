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

public class rdfQueryServlet extends HttpServlet {
  String queryString = "";
  String retType = "";
  String resultIdentifier="";
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
      queryString = request.getParameter("query");
      retType = request.getParameter("return");
      doGet(request, response);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
      queryString = request.getParameter("query");
      retType = request.getParameter("return");

      try { 
         PrintWriter out = response.getWriter();

         response.setContentType("text/html");
         response.setHeader("Cache-Control", "no-cache");
 
         if (queryString.indexOf(".")>-1) {
           resultIdentifier = queryString.substring(0, queryString.indexOf("."));
         } else {
           resultIdentifier = queryString;
         }
         String docMetadata = resolveIdentifier(resultIdentifier, retType);
         out.println(docMetadata);
         out.close();
 
      } catch (Exception e) { }
  }

  public String resolveIdentifier(String identifier, String retType) {
    String charset = "UTF-8";

    // String serverAddr = "http://boots.lanl.gov:10035/repositories/SI";
    
    // String graphName = "http://int.lanl.gov/SI";
    // String serverAddr = "http://localhost:8088/";
    
    String retValue = "";
    String query="";
    try {
      if (retType.equals("simple")) { 
        // query = "select * where { { ?s ?p ?o } filter (str(?s) = \"" + identifier + "\") }";
        identifier = identifier.replace("info-lanl-repo_","");
        query = "select * where { { ?s ?p ?o } filter (regex(str(?s), \"" + identifier + "\")) }";
      } else {
        // query = "select * WHERE { {  ?x <http://purl.org/dc/elements/1.1/title> ?y.  ?x <http://purl.org/dc/elements/1.1/description> ?z. OPTIONAL {?x <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?m. } } filter (str(?x)=\"" + identifier + "\") }";
        query = "select * WHERE { {  ?x <http://purl.org/dc/elements/1.1/title> ?y.  ?x <http://purl.org/dc/elements/1.1/description> ?z. OPTIONAL {?x <http://purl.org/dc/elements/1.1/creator> _:c.  _:c ?l ?m. } } filter (regex(str(?x), \"" + identifier + "\")) }";
      }
      System.out.println(query);
      String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
      // URL url = new URL(serverAddr + "sparql/?query=" + encodedQuery + "&default-graph-uri="+graphName);
      // allegrograph version
      URL url = new URL(serverAddr + "?query=" + encodedQuery);
      URLConnection connection = url.openConnection();
      connection.setRequestProperty("Accept-Charset", charset);
      InputStream response = connection.getInputStream();
      String results = fromStream(response);
      // retValue = results.substring(results.indexOf("<literal>"), results.indexOf("</literal>"));
      StringBuffer resultSet = new StringBuffer();
      int i = results.indexOf("<literal>");
      while (i >= 0) {
        resultSet.append(results.substring(results.indexOf("<literal>", i), results.indexOf("</literal>", i+1)));
        i = results.indexOf("<literal>", i+1);
        resultSet.append("<p>");
      }

      results = results.replaceAll("<", "&lt;");
      results = results.replaceAll(">", "&gt;");
      resultSet.append("<pre>"+ results + "</pre>");
      
      retValue = resultSet.toString();
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

}
