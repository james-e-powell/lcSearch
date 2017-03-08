package gov.lanl.lcSearch;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;

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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
                
import org.jfree.chart.ChartFactory;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;


public class lcChartServlet extends HttpServlet {
    // form input fields include:
    // solr query string
    // int specifying the number of records to be retrieved for this collection
    //

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
      System.out.println("doing get");
      String queryString = request.getParameter("query");
      String indexName = "index";

      String field = "contents";
      String fields[] = {"contents"};
      int hitsPerPage = 5;
      // int numHits = 100;
      System.out.println(queryString);

      try { 
         int numHits = Integer.valueOf(request.getParameter("numresults"));

         response.setContentType("image/png");
         response.setHeader("Cache-Control", "no-cache");

         System.out.println("opening index file " + indexName);
         IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexName)));
         IndexSearcher searcher = new IndexSearcher(reader);
         Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
         System.out.println("loaded index");

         MoreLikeThis mlt = new MoreLikeThis(reader);
         mlt.setFieldNames(new String[]{"contents"});
         mlt.setAnalyzer(analyzer);
         mlt.setBoost(true);

         StringReader qreader = new StringReader(queryString);

         StringReader queryreader = new StringReader(queryString);
         Query query = mlt.like(queryreader, null);

         TopDocs results = searcher.search(query, numHits);
         ScoreDoc[] hits = results.scoreDocs;

         int numTotalHits = results.totalHits;

         HashMap<String,Float> resultsMap = new HashMap<String,Float>();     
         for (int i=1; i<numHits; i++) { 
           String aResult = searcher.doc(hits[i].doc).toString();
           String resultFilePath = aResult.substring((aResult.indexOf("path:")+5), aResult.length()-2);
           String resultFilename = resultFilePath.substring(resultFilePath.lastIndexOf("/")+1, resultFilePath.length());
           String resultUri = "http://boots.lanl.gov/si/all/"+resultFilename;
           
           resultsMap.put(resultFilename,hits[i].score);
         }
         HashMap<String,Float> sortedResultsMap = getSortedHashMap(resultsMap);
         XYSeries resultsSeries = vertexXYSeries(sortedResultsMap);

         OutputStream outputStream = response.getOutputStream();
         String title = "Plot of doc scores";
         JFreeChart chart = getLine(resultsSeries, title);
         int width = 350;
         int height = 225;
         ChartUtilities.writeChartAsPNG(outputStream, chart, width, height);

         outputStream.close();

     } catch (Exception e) { System.out.println("Error "+e); }




	
  }

    public HashMap getSortedHashMap(HashMap<String,Float> input){
      Map tempMap = new HashMap();
      for (String wsState : input.keySet()){
        tempMap.put(wsState,input.get(wsState));
      }
      List mapKeys = new ArrayList(tempMap.keySet());
      List unsortedMapValues = new ArrayList(tempMap.values());
      List sortedMapValues = new ArrayList(unsortedMapValues);
      // Collections.sort(sortedMapValues);
      Collections.sort(sortedMapValues, Collections.reverseOrder());
      HashMap sortedMap = new LinkedHashMap();
      Object[] sortedArray = sortedMapValues.toArray();
      int size = sortedArray.length;
      for (int i=1; i < size ; i++){
        Float value = (Float)sortedArray[i];
        int mapValueIndex = unsortedMapValues.indexOf(value);
        sortedMap.put(mapKeys.get(mapValueIndex),
        (Float)sortedArray[i]);
        mapKeys.remove(mapValueIndex);
        unsortedMapValues.remove(mapValueIndex);
      }
      return sortedMap;
    }

    public JFreeChart getLine(XYSeries seriesParam, String titleParam) {
      XYDataset xyDataset = new XYSeriesCollection(seriesParam);
      JFreeChart chart = ChartFactory.createXYLineChart
          (titleParam, "Document", "Score",
             xyDataset, PlotOrientation.VERTICAL, true, true, false);
      return chart;
    }

    public XYSeries vertexXYSeries(HashMap<String,Float> vdDMap) {
      XYSeries series = new XYSeries("doc scores", true, false);
      List keys = new ArrayList(vdDMap.keySet());
      int count = 0;
      for (Object value : vdDMap.values()) {
        double dblValue = (Float)value;
        series.add(count, dblValue);
        count++;
      }
      return series;
  }


}

