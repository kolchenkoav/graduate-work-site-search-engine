package searchengine.parsing.sitemapping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.Messages;
import searchengine.model.Page;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.parsing.ParseLemma;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class SiteParser {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final ParseLemma parseLemma;

    private int siteId;
    private String domain;
    private String url;
    private ParsePageTask parsePageTask;

    private static AtomicBoolean cancelled = new AtomicBoolean(false);

    public static void setCancel(boolean b) {
        cancelled.set(b);
    }

    public static boolean getCancel() {
        return cancelled.get();
    }

    public void initSiteParser(int siteId, String domain, String url) {
        this.siteId = siteId;
        this.domain = domain;
        this.url = url;
    }

    public void forceStop() {
        setCancel(true);
        pool.shutdownNow();
    }

    private static final int PARALLELISM = 120;
    private ForkJoinPool pool = new ForkJoinPool(PARALLELISM);

    /**
     * Парсинг страниц
     */
    public void getLinks() {
        pool = new ForkJoinPool(PARALLELISM);
        parsePageTask = preparePage();
        pool.execute(parsePageTask);

        while (!parsePageTask.isDone() && !getCancel()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }

        if (getCancel()) {
            pool.shutdownNow();
            forceStop();
            log.info("Отмена индексации... ");
        } else {
            pool.shutdown();
        }
        try {
            parsePageTask.join();
            saveSite();
        } catch (Exception e) {
            log.error("parsePage.join() {}", e.getMessage());
        }
        parsePageTask = null;
    }

    private ParsePageTask preparePage() {
        parsePageTask = new ParsePageTask(parseLemma, pageRepository);

        parsePageTask.setUrl(url);
        parsePageTask.setDomain(domain);
        parsePageTask.setParent(null);
        parsePageTask.setSiteId(siteId);
        return parsePageTask;
    }

    private void saveSite() {
        SiteE siteE = siteRepository.findById(siteId).orElse(null);
        if (siteE == null) {
            log.warn("Сайт с ID: {} не найден", siteE);
            return;
        }
        siteE.setStatus(getCancel() ? Status.FAILED : Status.INDEXING);
        siteE.setStatusTime(Utils.setNow());

        getLemmasForAllPages(siteE);

        siteE.setStatus(getCancel() ? Status.FAILED : Status.INDEXED);
        siteE.setLastError(getCancel() ? Messages.INDEXING_STOPPED_BY_USER : "");
        siteE.setStatusTime(Utils.setNow());
        siteRepository.save(siteE);
        log.info("===>>> site '{}' saved", siteE.getName());
    }

    public void getLemmasForAllPages(SiteE siteE) {
        List<Page> pageList = pageRepository.findBySiteIdAndCode(siteE.getSiteId(), 200);
        parseLemma.setBeginPos(pageList.get(0).getPageId());
        parseLemma.setEndPos(pageList.get(pageList.size() - 1).getPageId());

        pageList.stream().takeWhile(e -> !getCancel()).forEach(this::parseSinglePage);
    }

    /**
     * Для отдельной страницы парсятся леммы и происходит запись лемм и индексов
     *
     * @param page - страница
     */
    public void parseSinglePage(Page page) {

        parseLemma.setCurrentPos(page.getPageId());
        if (!getCancel()) {
            parseLemma.parsing(page);
        }
    }

    public void clearUniqueLinks() {
        ParsePageTask.clearUniqueLinks();
    }

    public Page savePage(String url, SiteE siteE, String domain) {
        Document doc = parsePageTask.getDocumentByUrl(url);
        parsePageTask.setSiteId(siteE.getSiteId());
        parsePageTask.setDomain(domain);
        parsePageTask.setUrl(url);
        return parsePageTask.savePage(doc);
    }
}
