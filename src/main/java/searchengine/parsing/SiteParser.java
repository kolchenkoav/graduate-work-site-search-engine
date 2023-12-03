package searchengine.parsing;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.model.SiteE;
import searchengine.model.Status;

import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingServiceImpl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class SiteParser {
    private final ParseLemma parseLemma;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static ForkJoinPool pool;

    private int siteId;
    private String domain;
    private String url;
    private ParsePage parsedMap;

    public static void forceStop() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    public void getLinks() {
        pool = new ForkJoinPool(100);

        parsedMap = new ParsePage(parseLemma, pageRepository);
        parsedMap.setUrl(url);
        parsedMap.setDomain(domain);
        parsedMap.setParent(null);
        parsedMap.setLinks(new ArrayList<>());
        parsedMap.setLevel(0);
        parsedMap.setSiteId(siteId);

        pool.execute(parsedMap);
        do {
//            System.out.printf("\rActive threads: %d     Task count: %d    Steal count: %d     Run count: %d",
//                    pool.getActiveThreadCount(), pool.getQueuedTaskCount(), pool.getStealCount(), pool.getRunningThreadCount());
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                //logger.error(ExceptionUtils.getStackTrace(e));
            }
        } while (!parsedMap.isDone() && !parsedMap.isCancelledFromTask());

        if (parsedMap.isCancelledFromTask()) {
            pool.shutdownNow();
            forceStop();
            log.info("Отмена индексации... ");
            //return;
        } else {
            pool.shutdown();
        }

        List<String> results = parsedMap.join();

        SiteE siteE = siteRepository.findById(siteId).orElse(null);
        if (siteE == null) {
            log.warn("Сайт с ID: {} не найден", siteE);
            return;
        }

        if (parsedMap.isCancelledFromTask()) {
            siteE.setStatus(Status.FAILED);
        } else {
            siteE.setStatus(Status.INDEXED);
        }
        siteE.setStatusTime(new Timestamp(System.currentTimeMillis()));
        siteRepository.save(siteE);

        System.out.println();
        if (!results.isEmpty()) {
            System.out.printf("%s: %d links found.\n", results.get(0), results.size());
        }
        parsedMap = null;
    }
}
