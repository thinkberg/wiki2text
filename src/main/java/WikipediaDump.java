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

import java.util.Random;

/**
 * Create a plain text version of a wikipedia dump with a max size of 1G
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

    int linelength = -1;
    boolean strip = false;

    Random random = new Random(System.nanoTime());

    public ArticleFilter(long max, int linelength, boolean strip) {
      this.max = max;
      this.linelength = linelength;
      this.strip = strip;
    }

    public void process(WikiArticle page, Siteinfo siteinfo) throws SAXException {
      if (page.isMain()) {
        String text = wikiModel.render(new PlainTextConverter(true), page.getText());
        for (String l : text.split("\n")) {
          l = l.replaceAll("https?://[^/\\p{Space}]+(?:/[/a-zA-Z0-9_=\\-?~%,.!$()+*|\\\\\\\"\\'#]*)?", " ");
          l = l.replaceAll("\\{\\{.*?\\}\\}", " ");
          l = l.replaceAll("&[^;]+;", " ");
          if(strip) {
            l = l.replaceAll("\\b(\\p{L}\\p{Ll}*[\\p{Digit}\\p{Upper}]+\\p{L}*|[\\p{Upper}]{2,})\\b|[\\p{Punct}\\d]+", " ");
          }
          if (cnt < max) {
            if (l.split(" ").length > MIN_WORDS) {
              if(linelength != -1) {
                StringBuilder line = new StringBuilder();
                int randomLength = random.nextInt(linelength - 1) + 10;
                for(String word: l.split("\\p{Space}|\\p{Punct}")) {
                  if(word.trim().length() == 0) continue;
                  if(line.length() + word.length() > randomLength) break;
                  line.append(word).append(" ");
                }
                l = line.toString().trim();
              }
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
    if (args.length < 1) {
      System.err.println("Usage: Wiki2Text size [-l<linelength>] <wikipedia dump bz2>");
      System.err.println("       -l also strips numbers and punctiation from text ...");
      System.exit(-1);
    }
    int arg = 0;
    long size = Long.parseLong(args[arg++]);
    int linelength = -1;
    boolean strip = false;
    if(args.length > 2 && args[arg].startsWith("-l")) {
      linelength = Integer.parseInt(args[arg++].substring(2));
      strip = true;
    }
    String dumpFileName = args[arg];
    try {
      IArticleFilter handler = new ArticleFilter(size, linelength, strip);
      WikiXMLParser wxp = new WikiXMLParser(dumpFileName, handler);
      wxp.parse();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
