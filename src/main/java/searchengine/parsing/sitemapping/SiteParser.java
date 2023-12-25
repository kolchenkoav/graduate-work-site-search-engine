package searchengine.parsing.sitemapping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.config.Messages;
import searchengine.model.Page;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.parsing.ParseLemma;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;
import java.util.Set;
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
    private ParsePage parsePage;

    private AtomicBoolean cancelledSite = new AtomicBoolean(false);

    public void initSiteParser(int siteId, String domain, String url) {
        this.siteId = siteId;
        this.domain = domain;
        this.url = url;
    }

    public void forceStop() {
        pool.shutdownNow();
        this.setCancelledSite(new AtomicBoolean(true));
    }

    private static final int PARALLELISM = 120;
    private static final ForkJoinPool pool = new ForkJoinPool(PARALLELISM);

    /**
     * <p>This is a simple description of the method. . .
     * <a href="http://www.supermanisthegreatest.com">Superman!</a>
     * </p>
     * @see <a href="http://www.link_to_jira/HERO-402">HERO-402</a>
     * @since 1.0
     */
    public void getLinks() {
        parsePage = preparePage();
        pool.execute(parsePage);

        while (!parsePage.isDone() && !parsePage.getCancelled().get()) {
        }

        if (parsePage.getCancelled().get()) {
            pool.shutdownNow();
            forceStop();
            log.info("Отмена индексации... ");
            setCancelledSite(new AtomicBoolean(true));
        } else {
            pool.shutdown();
        }
        try {
            parsePage.join();
            saveSite();
        } catch (Exception e) {
            log.error("parsePage.join() {}", e.getMessage());
        }
        parsePage = null;
    }

    private ParsePage preparePage() {
        parsePage = new ParsePage(parseLemma, pageRepository);

        parsePage.setUrl(url);
        parsePage.setDomain(domain);
        parsePage.setParent(null);
        parsePage.setSiteId(siteId);
        return parsePage;
    }

    private void saveSite() {
        SiteE siteE = siteRepository.findById(siteId).orElse(null);
        if (siteE == null) {
            log.warn("Сайт с ID: {} не найден", siteE);
            return;
        }
        siteE.setStatus(parsePage.isCancelled() || parsePage.getCancelled().get() ? Status.FAILED : Status.INDEXING);
        siteE.setStatusTime(Utils.setNow());

        getLemmasForAllPages(siteE);

        siteE.setStatus(getCancelledSite().get() || parsePage.isCancelled() || parsePage.getCancelled().get() ? Status.FAILED : Status.INDEXED);
        siteE.setLastError(getCancelledSite().get() || parsePage.isCancelled() || parsePage.getCancelled().get() ? Messages.INDEXING_STOPPED_BY_USER : "");
        siteE.setStatusTime(Utils.setNow());
        siteRepository.save(siteE);
        log.info("===>>> site '{}' saved", siteE.getName());
    }

    public void getLemmasForAllPages(SiteE siteE) {
        List<Page> pageList = pageRepository.findBySiteIdAndCode(siteE.getSiteId(), 200);
        parseLemma.setBeginPos(pageList.get(0).getPageId());
        parseLemma.setEndPos(pageList.get(pageList.size()-1).getPageId());

        pageList.stream().takeWhile(e -> !this.getCancelledSite().get()).forEach(this::parseSinglePage);
    }

    public void parseSinglePage(Page page) {
        parseLemma.setCurrentPos(page.getPageId());
        if (!getCancelledSite().get()) {
            parseLemma.parsing(page);
        }
    }
}
