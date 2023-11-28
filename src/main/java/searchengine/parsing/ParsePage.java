package searchengine.parsing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

@Slf4j
@Component
@RequiredArgsConstructor
//@NoArgsConstructor
@Getter
@Setter
@Scope("prototype")
public class ParsePage extends RecursiveTask<List<String>> {
    private String url;
    private String domain;
    private ParsePage parent;
    private List<ParsePage> links;
    private int level;
    int code = 200;

    private static final ConcurrentHashMap<String, ParsePage> uniqueLinks = new ConcurrentHashMap<>();


    private String getLang(String beginHtml) {
        String result = "";
        try {
            int indexBegin = beginHtml.indexOf("lang=");
            result = beginHtml.substring(indexBegin + 6, indexBegin + 8);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return result;
    }

//    public ParsePage(String url, ParsePage parent) {
//        this(url, parent.domain);
//        this.parent = parent;
//        this.level = parent.level + 1;
//    }
//
//    public ParsePage(String url, String domain) {
//        this.url = url;
//        this.domain = domain;
//        this.parent = null;
//        this.links = new ArrayList<>();
//        this.level = 0;
//    }

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
            log.warn(e.getMessage());
        } catch (IOException | InterruptedException e) {
            log.warn(e.getMessage());
            return list;
        }

        String content = doc.body().text().substring(1, 150);
        String lang = getLang(doc.html().substring(1, 50));
        System.out.println("lang: " + lang + " code: " + code + " content" + content);
        System.out.println();


        Elements elements = doc.select("a[href~=^/?([\\w\\d/-]+)?]");
        for (Element link : elements) {
            String checkUrl = link.attr("abs:href").replace("//www.", "//");
            if (checkUrl.startsWith(domain)) {
                if (checkUrl.isEmpty() || checkUrl.contains("#")) {
                    continue;
                }

                if (!checkAddUrl(checkUrl)) {
                    list.add(checkUrl);
                    ParsePage parentX = this;
                    System.out.println("level: "+this.level +" domain: "+this.domain+" parent: "+this.parent);
                    ParsePage newParse = new ParsePage();

//    public ParsePage(String url, ParsePage parent) {
//        this(url, parent.domain);
//        this.parent = parent;
//        this.level = parent.level + 1;
//    }
//
//    public ParsePage(String url, String domain) {
//        this.url = url;
//        this.domain = domain;
//        this.parent = null;
//        this.links = new ArrayList<>();
//        this.level = 0;
//    }

                    //ParsePage newParse = new ParsePage(checkUrl, this);


                    newParse.setUrl(checkUrl);
                    newParse.setDomain(parent.domain);
                    newParse.setParent(parentX);
                    newParse.setLevel(parent.level + 1);
                    newParse.fork();
                    tasks.add(newParse);
                    links.add(newParse);
                }
            }
        }

        try {
            addResultsFromTasks(list, tasks);
        } catch (RuntimeException e) {
            log.warn(e.getMessage());
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

}
