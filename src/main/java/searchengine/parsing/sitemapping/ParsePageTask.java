package searchengine.parsing.sitemapping;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;;
import searchengine.parsing.ParseLemma;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

import static searchengine.parsing.sitemapping.Utils.*;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class ParsePageTask extends RecursiveTask<Set<String>> {
    private ParseLemma parseLemma;
    private PageRepository pageRepository;

    public ParsePageTask(ParseLemma parseLemma, PageRepository pageRepository) {
        this.parseLemma = parseLemma;
        this.pageRepository = pageRepository;
    }

    private int siteId;
    private String url;
    private String domain;
    private ParsePageTask parent;
    private int code = 200;
    private int countErrorPages = 0;

    private static ConcurrentHashMap<String, ParsePageTask> uniqueLinks = new ConcurrentHashMap<>();


    /**
     * Возвращает список уникальных ссылок для сайта
     */
    @Override
    protected Set<String> compute() {
        Set<String> listOfUrls = new HashSet<>();
        List<ParsePageTask> tasks = new ArrayList<>();

        Document doc = getDocumentByUrl(url);

        if (doc == null) {
            return listOfUrls;
        }
        if (uniqueLinks.containsKey(url)) {
            savePage(doc);
            printMessageAboutPages();
        }

        Elements elements = doc.select("a[href~=^/?([\\w\\d/-]+)?]");
        for (Element link : elements) {
            String checkingUrl = link.attr("abs:href").replace("//www.", "//");
            if (isValidUrl(checkingUrl)) {
                listOfUrls.add(checkingUrl);

                ParsePageTask newParsePageTask = prepareNewPage(checkingUrl);

                newParsePageTask.fork();
                tasks.add(newParsePageTask);
            }
        }

        tasks.forEach((task) -> listOfUrls.addAll(task.join()));
        return listOfUrls;
    }

    /**
     * Получить Document по ссылке
     *
     * @param url ссылка на страницу
     * @return Document
     */
    public Document getDocumentByUrl(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
        } catch (HttpStatusException e) {
            code = e.getStatusCode();
        } catch (IOException ex) {
            return null;
        }
        return doc;
    }

    /**
     * Сохраняет новую страницу
     *
     * @param doc документ(страница) для сохранения
     */
    public Page savePage(Document doc) {
        if (doc == null) {
            log.warn("Failed to save page");
            return null;
        }
        String content = "";
        try {
            content = doc.body().text();
        } catch (Exception e) {
            log.warn("Ошибка при получении контекста страницы: {}", url);
        }

        String title = "";
        try {
            title = doc.title();
        } catch (Exception e) {
            log.warn("Ошибка при получении заголовка страницы: {}", url);
        }

        String s = url.substring(domain.length());
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        Page page = new Page(siteId, s, code, content, title);

        pageRepository.save(page);

        return page;
    }

    private void printMessageAboutPages() {
        if (code != 200) {
            countErrorPages++;
            log.warn("url: {} {}", url, code);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Number of pages found: ").append(ANSI_BLUE).append(uniqueLinks.size()).append(ANSI_RESET);
        if (countErrorPages > 0) {
            builder.append(" Pages with errors ").append(ANSI_RED).append(countErrorPages).append(ANSI_RESET);
        }
        System.out.print(builder + "\r");
    }

    private ParsePageTask prepareNewPage(String checkingUrl) {
        ParsePageTask newParse = new ParsePageTask(parseLemma, pageRepository);
        newParse.setUrl(checkingUrl);
        newParse.setParent(this);
        newParse.setDomain(domain);
        newParse.setSiteId(siteId);
        return newParse;
    }

    private boolean isValidUrl(String checkingUrl) {
        if (checkingUrl.startsWith(domain)) {
            if (checkingUrl.isEmpty() ||
                    checkingUrl.contains("#") ||
                    checkingUrl.contains(".jpg") ||
                    checkingUrl.contains(".png")) {
                return false;
            }
            return !isExistUrlInUniqueLinks(checkingUrl);
        }
        return false;
    }

    private boolean isExistUrlInUniqueLinks(String url) {
        boolean isExist = uniqueLinks.containsKey(url);
        if (!isExist) {
            uniqueLinks.put(url, this);
        }
        return isExist;
    }

    public static void clearUniqueLinks() {
        uniqueLinks.clear();
    }
}


