package searchengine.parsing.siteMapping;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.parsing.ParseLemma;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static searchengine.parsing.siteMapping.Utils.*;


@Slf4j
@Getter
@Setter
@Component
@Scope("prototype")
public class ParsePage extends RecursiveTask<Set<String>> {
    private final ParseLemma parseLemma;
    private final PageRepository pageRepository;

    public ParsePage(ParseLemma parseLemma, PageRepository pageRepository) {
        this.parseLemma = parseLemma;
        this.pageRepository = pageRepository;
    }

    private int siteId;
    private String url;
    private String domain;
    private ParsePage parent;
    private int code = 200;
    private int countErrorPages = 0;

    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private static ConcurrentHashMap<String, ParsePage> uniqueLinks = new ConcurrentHashMap<>();

    @Override
    protected Set<String> compute() {
        Set<String> listOfUrls = new HashSet<>();
        List<ParsePage> tasks = new ArrayList<>();

        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
            TimeUnit.MILLISECONDS.sleep((int) (Math.random() * 50 + 100));
        } catch (HttpStatusException e) {
            code = e.getStatusCode();
        } catch (IOException | InterruptedException ex) {
            return listOfUrls;
        }

        if (doc == null) {
            return listOfUrls;
        }
        if (uniqueLinks.containsKey(url)) {
            savePage(doc);
        }

        Elements elements = doc.select("a[href~=^/?([\\w\\d/-]+)?]");
        for (Element link : elements) {
            String checkingUrl = link.attr("abs:href").replace("//www.", "//");
            if (checkUrl(checkingUrl)) {
                listOfUrls.add(checkingUrl);

                ParsePage newParsePage = prepareNewPage(checkingUrl);

                newParsePage.fork();
                tasks.add(newParsePage);
            }
        }

        tasks.forEach((task) -> listOfUrls.addAll(task.join()));
        return listOfUrls;
    }

    private void savePage(Document doc) {
        String content = doc.body().text();
        String title = doc.title();
        Page page = new Page(siteId, url.substring(domain.length(), url.length()), code, content, title);
        page = pageRepository.save(page);

        if (code == 200) {
            System.out.print("Количество найденных страниц: " + ANSI_YELLOW + uniqueLinks.size() +
                    ANSI_RESET + " Страницы с ошибками: " + ANSI_RED +  countErrorPages + ANSI_RESET+"\r");
            //TODO parseLemma.parsing(content, siteId, page.getPageId());
            parseLemma.parsing(content, siteId, page.getPageId());
        } else {
            countErrorPages++;
            log.warn("url: {} {}", url, code);
        }
    }

    private ParsePage prepareNewPage(String checkingUrl) {
        ParsePage newParse = new ParsePage(parseLemma, pageRepository);
        newParse.setUrl(checkingUrl);
        newParse.setParent(this);
        newParse.setDomain(domain);
        newParse.setSiteId(siteId);
        return newParse;
    }

    private boolean checkUrl(String checkingUrl) {
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

    public void clearUniqueLinks() {
        uniqueLinks.clear();
    }
}


