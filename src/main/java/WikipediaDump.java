/*
 * Copyright (c) 2012 TWIMPACT UG (haftungsbeschraenkt). All rights reserved.
 */


import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import org.xml.sax.SAXException;

/**
 * >>Describe Class<<
 *
 * @author Matthias L. Jugel
 */
public class WikipediaDump {

  /**
   * Print title an content of all the wiki pages in the dump.
   */
  static class ArticleFilter implements IArticleFilter {

    private static final int MIN_WORDS = 40;
    WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");

    long max = 0L;
    long cnt = 0L;

    public ArticleFilter(long bytes) {
      max = bytes;
    }

    public void process(WikiArticle page, Siteinfo siteinfo) throws SAXException {
      if (page.isMain()) {
        String text = wikiModel.render(new PlainTextConverter(true), page.getText());
        for (String l : text.split("\n")) {
          l = l.replaceAll("https?://[^/\\p{Space}]+(?:/[/a-zA-Z0-9_=\\-?~%,.!$()+*|\\\\\\\"\\'#]*)?", " ");
          l = l.replaceAll("\\{\\{.*?\\}\\}", " ");
          if (cnt < max) {
            if (l.split(" ").length > MIN_WORDS) {
              System.out.println(l);
              System.err.print("\r" + cnt + " ("+ (100 * cnt / max)+ "%)");
              cnt += l.length();
            }
          } else {
            System.err.println();
            System.exit(0);
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Usage: Parser <XML-FILE>");
      System.exit(-1);
    }
    // String bz2Filename =
    // "c:\\temp\\dewikiversity-20100401-pages-articles.xml.bz2";
    String bz2Filename = args[0];
    try {
      IArticleFilter handler = new ArticleFilter(1073741824L);
      WikiXMLParser wxp = new WikiXMLParser(bz2Filename, handler);
      wxp.parse();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
