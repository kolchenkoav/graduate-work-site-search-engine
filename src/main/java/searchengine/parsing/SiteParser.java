package searchengine.parsing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class SiteParser {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final ParseLemma parseLemma;                // = new ParseLemma();

    private int siteId;
    private String domain;
    private String url;
    private ParsePage parsePage;

    public void initSiteParser(int siteId, String domain, String url) {
        this.siteId = siteId;
        this.domain = domain;
        this.url = url;
    }

    public static void forceStop() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    private static final int PARALLELISM = 120;
    private static ForkJoinPool pool;

    public void getLinks() {
        System.out.println();
        System.out.println(url);
        pool = new ForkJoinPool(PARALLELISM);

        parsePage = preparePage();
        pool.execute(parsePage);

        while (!parsePage.isDone() && !parsePage.getCancelled().get()) {
        }

        if (parsePage.getCancelled().get()) {
            pool.shutdownNow();
            forceStop();
            log.info("Отмена индексации... ");
        } else {
            pool.shutdown();
        }
        Set<String> result = parsePage.join();

        saveSite();
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
        siteE.setStatus(parsePage.isCancelled() || parsePage.getCancelled().get() ? Status.FAILED : Status.INDEXED);
        siteE.setStatusTime(Utils.setNow());
        siteRepository.save(siteE);
        log.info("Save: {}", siteE.getName());
    }
}
