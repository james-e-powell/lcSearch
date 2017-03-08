package gov.lanl.lcSearch;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.queries.mlt.*;

public class lcSearchServlet extends HttpServlet {
    // form input fields include:
    // solr query string
    // int specifying the number of records to be retrieved for this collection
    //
    //
    //
  String queryString = "";

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
      doGet(request, response);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
      System.out.println("doing get");
      if (!(queryString.equals(""))) {
        queryString = request.getParameter("query");
      }
      String indexName = "index";

      String field = "contents";
      String fields[] = {"contents"};
      int hitsPerPage = 5;
      // int numHits = 100;
      System.out.println(queryString);

      try { 
         int numHits = Integer.valueOf(request.getParameter("numresults"));
         PrintWriter out = response.getWriter();

         response.setContentType("text/html");
         response.setHeader("Cache-Control", "no-cache");

         System.out.println("opening index file " + indexName);
         IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexName)));
         IndexSearcher searcher = new IndexSearcher(reader);
         Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
         System.out.println("loaded index");

         Set<String> stopWords = new HashSet<String>() {{
            add("a"); add("an"); add("the");
            add("is"); add("are"); add("was"); add("were");
            add("this"); add("these"); add("that"); add("those");
         }};

         MoreLikeThis mlt = new MoreLikeThis(reader);
         mlt.setFieldNames(new String[]{"contents"});
         mlt.setAnalyzer(analyzer);
         mlt.setBoost(true);
         mlt.setStopWords(stopWords);

         StringReader queryreader = new StringReader(queryString);
         StringReader qreader = new StringReader(queryString);

         out.println("<html><head><title>SI Search Results</title>");
         out.println("    <script src=\"/lcSearch/static/scripts/js/jquery/jquery-1.9.0.min.js\"></script>");
         out.println("    <script type=\"text/javascript\">");
         out.println("      function selectAll() {");
         out.println("         $( '[type=checkbox]').prop('checked', true); ");
         out.println("      }");
         out.println("    </script>");
         out.println("    <style type=\"text/css\">");
         out.println("      body {font-family: Arial, Helvetica, sans-serif}");
         out.println("    </style>");
         out.println("</head>");
         out.println("<body>");

         Query query = mlt.like(queryreader, null);

         TopDocs results = searcher.search(query, numHits);
         ScoreDoc[] hits = results.scoreDocs;
         String encodedQuery = URLEncoder.encode(queryString, "UTF-8");

         // out.println("<form action=\"/lcSearch/lcgetids\" method=\"post\">");
         out.println("<form action=\"/lcSearch/d3Network.html\" method=\"get\">");
         // out.println("<input type=\"hidden\" name=\"return\" value=\"simple\">");
         // out.println("<table style=\"border-style:solid\"><tr>");
         out.println("<table><tr>");

         // out.println("<iframe src=\"http://boots.lanl.gov:8080/lcSearch/lcchart?query="+encodedQuery+"&numresults="+numHits+"\"></iframe>");

    
         int numTotalHits = results.totalHits;
         out.println("<td><img src=\"/lcSearch/si_icon.png\"><br>");
         out.println(numTotalHits + " total matching documents<br>");
         if (numTotalHits == 0) {
           out.print("<p><font color=\"red\">This is a full text to full text search tool. For best results, paste in a few paragraphs of content that represents ");
           out.println("the topic you are interested in.</font><p>");
         }
         out.println("top " + numHits + " displayed<p>");
         out.println("<a href=\"http://boots.lanl.gov:8080/lcSearch/search.html\"><b>New Search</b></a></td>");
         out.println("<td colspan=\"2\" align=\"center\"><img src=\"http://boots.lanl.gov:8080/lcSearch/lcchart?query="+encodedQuery+"&numresults="+numHits+"\" /></td>");
         out.println("</tr>");

         out.println("<tr><td colspan=\"3\">");
         out.println("<b>Search based on, but not limited to these terms:</b><br>");
         String[] goodTerms = mlt.retrieveInterestingTerms(qreader, field);
         out.println("<i>");
         for (int j=1; j<goodTerms.length; j++) {
           if (j==1) {
             out.println(goodTerms[j]);

           } else {
             out.println(", "+goodTerms[j]);
           }
         }
         // out.println("</i>");
         out.println("</td></tr>");

         out.println("<tr><th colspan=\"3\" bgcolor=\"#E8D1D1\">Display representation of some or <input name=\"All\" type=\"button\" value=\"All\" onclick=\"selectAll();\"> records below <input type=\"submit\"> as <input type=\"radio\" name=\"return\" value=\"report\" checked> report, <input type=\"radio\" name=\"return\" value=\"network\" >network, <input type=\"radio\" name=\"return\" value=\"tagcloud\">tagcloud, <input type=\"radio\" name=\"return\" value=\"sankey\">sankey diagram</th></tr>");

         out.println("<tr><td colspan=\"3\">");
         out.println("<table>");

         out.println("<tr><th>Score&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th><th>Document link</th><th>Title</th></tr>");
         // out.println("<form action=\"/lcSearch/lcgetids\" method=\"post\">");
         out.println("<form action=\"/lcSearch/d3Network.html\" method=\"get\">");

         out.println("<tr>");
         for (int i=1; i<numHits; i++) { 
           String aResult = searcher.doc(hits[i].doc).toString();
           String resultFilePath = aResult.substring((aResult.indexOf("path:")+5), aResult.length()-2);
           String resultFilename = resultFilePath.substring(resultFilePath.lastIndexOf("/")+1, resultFilePath.length());
           String resultUri = "http://boots.lanl.gov/si/all/"+resultFilename;
           String resultIdentifier = resultFilename.substring(0, resultFilename.indexOf("."));
           String docTitle = resolveIdentifier(resultIdentifier);
           
           // out.println("<td bgcolor=\"#efefef\"><input type=\"checkbox\" name=\"ids\" value=\""+resultFilename+"\">" + hits[i].score + " : </td><td bgcolor=\"#efefef\"><a href=\""+resultUri+"\">" + resultFilename + "</a></td>");
           out.print("<td bgcolor=\"#efefef\"><input type=\"checkbox\" name=\"ids\" value=\""+resultFilename+"\">&nbsp;");
           out.print(String.format("%1.4f", hits[i].score));
           out.println("</td><td bgcolor=\"#efefef\"><a href=\""+resultUri+"\">" + resultFilename + "</a></td>");
           out.println("<td bgcolor=\"#efefef\"><a href=\"http://boots.lanl.gov:8080/lcSearch/lcrdfresults?return=other&query=" + resultFilename + "\">" + docTitle + "</a></td></tr>");
         }
         
         out.println("</form>");
         out.println("</table>");
         out.println("</td></tr>");
         out.println("</table>");

         reader.close();
         out.close();

     } catch (Exception e) { System.out.println("Error "+e); }


  }

  public String resolveIdentifier(String identifier) {
    String charset = "UTF-8";
    // String graphName = "http://int.lanl.gov/SI";
    // String serverAddr = "http://localhost:8088/";
    String retValue = "";
    try {
      // String query = "select * where { { ?s <http://purl.org/dc/elements/1.1/title> ?o } filter (str(?s) = \"" + identifier + "\") }";;
      identifier = identifier.replace("info-lanl-repo_","");
      String query = "select * where { { ?s <http://purl.org/dc/elements/1.1/title> ?o } filter (regex(str(?s), \"" + identifier + "\")) }";;
      System.out.println(query);
      String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
      // mulgara form
      // URL url = new URL(serverAddr + "sparql/?query=" + encodedQuery + "&default-graph-uri="+graphName);
      // allegrograph form
      URL url = new URL(serverAddr + "?query=" + encodedQuery);
      URLConnection connection = url.openConnection();
      connection.setRequestProperty("Accept-Charset", charset);
      InputStream response = connection.getInputStream();
      String results = fromStream(response);
      retValue = results.substring(results.indexOf("<literal>"), results.indexOf("</literal>"));
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

