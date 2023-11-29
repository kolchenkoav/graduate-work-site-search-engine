package searchengine.parsing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

public class ParsePage extends RecursiveTask<List<String>> {
    //    private static final Logger logger = LogManager.getLogger(ParsePage.class);
    private int siteId;
    private String url;
    private String domain;
    private ParsePage parent;
    private List<ParsePage> links = new ArrayList<>();
    private int level;
    int code = 200;

    private static final ConcurrentHashMap<String, ParsePage> uniqueLinks = new ConcurrentHashMap<>();

    public void setSiteId(int siteId) {
        //System.out.println("### ParsePage.setSiteId() -> siteId: " + siteId);
        this.siteId = siteId;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setParent(ParsePage parent) {
        this.parent = parent;
    }

    public void setLinks(List<ParsePage> links) {
        this.links = links;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    private String getLang(String beginHtml) {
        String result = "";
        try {
            int indexBegin = beginHtml.indexOf("lang=");
            result = beginHtml.substring(indexBegin + 6, indexBegin + 8);
        } catch (Exception e) {
            //log.warn(e.getMessage());
        }
        return result;
    }

    @Override
    protected List<String> compute() {
        List<String> list = new ArrayList<>();
        List<ParsePage> tasks = new ArrayList<>();

        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
            Thread.sleep((int) (Math.random() * 50 + 100));
        } catch (HttpStatusException e) {        //IOException | InterruptedException
            code = e.getStatusCode();
            //log.warn(e.getMessage());
        } catch (IOException | InterruptedException e) {
            //log.warn(e.getMessage());
            return list;
        }

        Elements elements = doc.select("a[href~=^/?([\\w\\d/-]+)?]");
        for (Element link : elements) {
            String checkUrl = link.attr("abs:href").replace("//www.", "//");
            if (checkUrl.startsWith(domain)) {
                if (checkUrl.isEmpty() || checkUrl.contains("#") || checkUrl.contains(".jpg") || checkUrl.contains(".png")) {
                    continue;
                }

                if (!checkAddUrl(checkUrl)) {
                    //===================
                    String content = doc.body().text();
                    String lang = getLang(doc.html().substring(1, 50));
                    //System.out.println("Add =>  lang: " + lang + " code: " + code + " content" + content.substring(1, 70));

                    Page page = new Page();
                    page.setSiteId(siteId);
                    page.setCode(code);
                    page.setPath(checkUrl);
                    page.setContent(content);
                    System.out.println("*** add   siteId: " + page.getSiteId() + " path: " + page.getPath() + " code: " + code);
                    //pageRepository.save(page);
                    //log.info("'{}' page has been added ", page.getPath());
                    //===================

                    list.add(checkUrl);

                    ParsePage newParse = new ParsePage();
                    newParse.setUrl(checkUrl);
                    newParse.setParent(this);
                    newParse.setDomain(domain);
                    newParse.setLinks(new ArrayList<>());
                    newParse.setLevel(level + 1);
                    newParse.setSiteId(siteId);

                    newParse.fork();
                    tasks.add(newParse);
                    links.add(newParse);
                }
            }
        }

        try {
            addResultsFromTasks(list, tasks);
        } catch (RuntimeException e) {
            //logger.warn(e);
        }
        return list;
    }

    private boolean checkAddUrl(String url) {
        boolean isExist = uniqueLinks.containsKey(url);
        if (!isExist) {
            uniqueLinks.put(url, this);
        }
        return isExist;
    }

    private void addResultsFromTasks(List<String> list, List<ParsePage> tasks) {
        for (ParsePage item : tasks) {
            list.addAll(item.join());
        }
    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("\t".repeat(this.level)).append(this.url).append("\n");
//        this.links.forEach(e -> sb.append(e.toString()));
//        return sb.toString();
//    }
}
