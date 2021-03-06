package com.samjsoares.soar.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.samjsoares.soar.constant.TimeConstants;
import com.samjsoares.soar.util.CollectionsUtil;
import com.samjsoares.soar.util.UrlUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Encapsulates a map from search term to set of TermProcessor.
 *
 * @author soar
 */
public class InMemoryIndexer implements Indexer {

  private Map<String, Set<TermProcessor>> index = new HashMap<>();
  private Map<String, Long> timeIndexed = new HashMap<>();

  /**
   * Adds a TermProcessor to the set associated with `term`.
   *
   * @param term
   * @param termProcessor
   */
  public void add(String term, TermProcessor termProcessor) {
    Set<TermProcessor> set = get(term);

    // if we're seeing a term for the first time, make a new Set
    if (set == null) {
      set = new HashSet<>();
      index.put(term, set);
    }
    // otherwise we can modify an existing Set
    set.add(termProcessor);
  }

  /**
   * Looks up a search term and returns a set of TermProcessors.
   *
   * @param term
   * @return
   */
  public Set<TermProcessor> get(String term) {
    return index.get(term);
  }

  /**
   * Add a page to the index.
   *
   * @param url        URL of the page.
   * @param document Collection of elements that should be indexed.
   */
  public void indexPage(String url, Document document) {
    if (document == null) {
      return;
    }

    url = UrlUtil.getUrlString(url);
    if (url == null) {
      return;
    }

    // make a TermProcessor and count the terms in the paragraphs
    TermProcessor tp = new TermProcessor(url);
    tp.processElements(document.children());

    // for each term in the TermProcessor, add the TermProcessor to the index
    for (String term : tp.keySet()) {
      add(term, tp);
    }

    timeIndexed.put(url, System.currentTimeMillis());
  }

  /**
   * Prints the contents of the index.
   */
  public void printIndex() {
    // loop through the search terms
    for (String term : keySet()) {
      System.out.println(term);

      // for each term, print the pages where it appears
      Set<TermProcessor> tcs = get(term);
      for (TermProcessor tc : tcs) {
        Integer count = tc.get(term).getCount();
        System.out.println("    " + tc.getUrl() + " " + count);
      }
    }
  }

  /**
   * Returns the set of terms that have been indexed.
   *
   * @return
   */
  private Set<String> keySet() {
    return index.keySet();
  }

  public boolean shouldIndex(String url) {
    url = UrlUtil.getUrlString(url);
    if (url == null) return false;
    Long lastIndexedTime = timeIndexed.get(url);

    if (lastIndexedTime == null) {
      return true;
    }

    long nextIndexTime = lastIndexedTime + TimeConstants.MS_PER_WEEK;
    return System.currentTimeMillis() > nextIndexTime;
  }

  public Map<String, Integer> getCounts(String term) {
    Set<TermProcessor> termProcessors = get(term);
    Map<String, Integer> counts = new HashMap<>();

    for (TermProcessor tp : CollectionsUtil.emptyIfNull(termProcessors)) {
      counts.put(tp.getUrl(), tp.getTermCount(term));
    }

    return counts;
  }

}