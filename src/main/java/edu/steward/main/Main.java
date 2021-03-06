package edu.steward.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.common.collect.ImmutableList;

import edu.steward.Sentiment.SentimentWrapper;
import edu.steward.handlers.html.AboutHandler;
import edu.steward.handlers.html.IndexHandler;
import edu.steward.handlers.html.StockHandler;
import edu.steward.handlers.html.UserHandler;
import edu.steward.handlers.html.WatchlistHandler;
import edu.steward.handlers.json.*;
import edu.steward.pools.Pool;
import edu.steward.sql.Update;
import freemarker.template.Configuration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;

import static edu.steward.Sentiment.Watchlist.trendingSentiments;

public class Main {
  private static final int DEFAULT_PORT = 4567;

  public static void main(String[] args) {
    new Main(args).run();
  }

  private String[] args;

  private Main(String[] args) {
    this.args = args;
  }

  private void run() {
    // Parse command line arguments
    // Workaround for redirect issue (#69) in yahoo finance library
    // System.setProperty("yahoofinance.baseurl.histquotes",
    // "https://ichart.yahoo.com/table.csv");
    // System.setProperty("yahoofinance.baseurl.quotes",
    // "http://download.finance.yahoo.com/d/quotes.csv");

//    TODO: Collect all the pool times from the db and instantiate pool Ends update function!!!!!

    Update.update();
    Integer timeInTen = (int) (System.currentTimeMillis() / 1000L) + 3000;
    Pool.setEndTimer("LHA3", timeInTen);
    OptionParser parser = new OptionParser();
    parser.accepts("gui");
    parser.accepts("port").withRequiredArg().ofType(Integer.class)
        .defaultsTo(DEFAULT_PORT);
    OptionSet options = parser.parse(args);

    if (options.has("gui")) {
      runSparkServer((Integer) options.valueOf("port"));
    }

    initiateSentimentsCache();

    System.out.println("!!!BEEP BOOP SERVER READY TO ROLL!!!");
  }

  public static void initiateSentimentsCache() {
    SentimentWrapper sw = new SentimentWrapper();
    trendingSentiments();
    sw.findSentimentOf("AAPL");
    sw.findSentimentOf("SNAP");
    sw.findSentimentOf("KO");
    sw.findSentimentOf("FB");
  }

  public static FreeMarkerEngine createEngine() {
    Configuration config = new Configuration();
    File templates = new File("src/main/resources/spark/template/freemarker");
    try {
      config.setDirectoryForTemplateLoading(templates);
    } catch (IOException ioe) {
      System.out.printf("ERROR: Unable use %s for template loading.%n",
          templates);
      System.exit(1);
    }
    return new FreeMarkerEngine(config);
  }

  private void runSparkServer(int port) {
    Spark.port(port);
    Spark.externalStaticFileLocation("src/main/resources/static");
    Spark.exception(Exception.class, new ExceptionPrinter());
    FreeMarkerEngine freeMarker = createEngine();

    // Spark routes
    // Pages
    Spark.get("/", new IndexHandler(), freeMarker);
    Spark.get("/about", new AboutHandler(), freeMarker);
    Spark.get("/stock/:ticker", new StockHandler(), freeMarker);
    Spark.get("/user/:id", new UserHandler(), freeMarker);
    Spark.get("/watchlist", new WatchlistHandler(), freeMarker);
    // Auth
    Spark.get("/login", new LoginHandler());
    Spark.get("/logout", new LogoutHandler());
    // JSON
    Spark.post("/suggest", new SuggestHandler(
        ImmutableList.of("data/allStocksNDQ.csv", "data/allStocks.csv")));
    Spark.post("/newPortfolio", new NewPortfolioHandler());
    Spark.post("/newPool", new NewPoolHandler());
    Spark.post("/joinPool", new JoinPoolHandler());
    Spark.post("/deletePortfolio", new DeletePortfolioHandler());
    Spark.post("/renamePortfolio", new RenamePortfolioHandler());
    Spark.post("/getPortfolioStocks", new GetPortfolioHandler());
    Spark.post("/stockAction", new StockActionHandler());
    Spark.post("/getGraphData", new GetGraphDataHandler());
    Spark.post("/getStockPrediction", new GetStockPredictionHandler());
    Spark.post("/getUnrealizedData", new GetUnrealizedDataHandler());
    SentimentWrapper wrap = new SentimentWrapper();
    Spark.post("/getSentiment", new GetSentimentHandler(wrap));
    Spark.post("/getLeaderboard", new GetLeaderboardHandler());
    Spark.post("/getNetWorthGraph", new GetNetWorthOverTime());
    Spark.post("/getCurrPrice", new GetCurrPriceHandler());
    Spark.post("/getPoolInfo", new GetPoolInfoHandler());
    Spark.post("/getTransactionHistory", new GetTransactionHistoryHandler());
    Spark.post("/leavePool", new LeavePoolHandler());
  }

  private static class ExceptionPrinter implements ExceptionHandler {
    @Override
    public void handle(Exception e, Request req, Response res) {
      res.status(500);
      StringWriter stacktrace = new StringWriter();
      try (PrintWriter pw = new PrintWriter(stacktrace)) {
        pw.println("<pre>");
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
      res.body(stacktrace.toString());
    }
  }
}
