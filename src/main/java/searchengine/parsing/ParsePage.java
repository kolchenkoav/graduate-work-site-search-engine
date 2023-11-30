package searchengine.parsing;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.lang.invoke.WrongMethodTypeException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class ParsePage extends RecursiveTask<List<String>> {
    private final PageRepository pageRepository;
    private int siteId;
    private String url;
    private String domain;
    private ParsePage parent;
    private List<ParsePage> links = new ArrayList<>();
    private int level;
    int code = 200;
    Connection.Response response = null;

    private static ConcurrentHashMap<String, ParsePage> uniqueLinks = new ConcurrentHashMap<>();

    public void clearUniqueLinks() {
        uniqueLinks.clear();
    }

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

    @Override
    protected List<String> compute() {
        List<String> list = new ArrayList<>();
        List<ParsePage> tasks = new ArrayList<>();

        Document doc = null;
        try {
            response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) YandexIndexingMachine")
                    .referrer("https://www.google.com")
                    .ignoreContentType(true)
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute();
            if (!response.contentType().startsWith("text/html;")) {
                throw new WrongMethodTypeException("wrong format");
            }
            code = response.statusCode();
            Thread.sleep((int) (Math.random() * 50 + 100));
            doc = response.parse();

        } catch (HttpStatusException e) {

        } catch (IOException | InterruptedException e) {
            return list;
        }

        String content = doc.body().text();
        Page page = new Page();
        page.setSiteId(siteId);
        page.setCode(code);
        page.setPath(url);
        page.setContent(content);
        pageRepository.save(page);
        log.info("{} ", page.getPath());
        //log.info("===> "+doc.body().text().substring(1, 100));

        Elements elements = doc.select("a[href~=^/?([\\w\\d/-]+)?]");
        for (Element link : elements) {
            String checkUrl = link.attr("abs:href").replace("//www.", "//");
            if (checkUrl.startsWith(domain)) {
                if (checkUrl.isEmpty() || checkUrl.contains("#") || checkUrl.contains(".jpg") || checkUrl.contains(".png")) {
                    continue;
                }

                if (!checkAddUrl(checkUrl)) {


                    list.add(checkUrl);

                    ParsePage newParse = new ParsePage(pageRepository);
                    newParse.setUrl(checkUrl);
                    newParse.setParent(this);
                    newParse.setDomain(domain);
                    newParse.setLinks(new ArrayList<>());
                    newParse.setLevel(level + 1);
                    newParse.setSiteId(siteId);

                    newParse.fork();
                    tasks.add(newParse);
                    links.add(newParse);
                } else {
                    //log.warn(String.valueOf(uniqueLinks.size()));
                }
            }
        }

        try {
            addResultsFromTasks(list, tasks);
        } catch (RuntimeException e) {
            //logger.warn(e);
        }
        tasks.clear();
        //uniqueLinks = null;
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
