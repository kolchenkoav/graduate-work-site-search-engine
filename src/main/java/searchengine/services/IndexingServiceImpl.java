package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Messages;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.controllers.DefaultController;
import searchengine.dto.indexing.IndexingErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.parsing.ParseLemma;

import searchengine.parsing.siteMapping.ParsePage;
import searchengine.parsing.siteMapping.SiteParser;
import searchengine.parsing.siteMapping.Utils;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.transaction.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final LemmaRepository lemmaRepository;
    private final SiteParser siteParser;
    private final ParsePage parsePage;
    private final SiteList siteListFromConfig;
    private final List<SiteE> siteEList = new ArrayList<>();
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final DefaultController controller;
    private Object response;
    private ThreadPoolExecutor executor;


    @Override
    public Object startIndexing() {
        parsePage.setCancelled(new AtomicBoolean(false));
        if (indexing()) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError(Messages.INDEXING_HAS_ALREADY_STARTED);
            response = responseFalse;
        }
        controller.index();
        return response;
    }

    private boolean indexing() {
        if (siteListFromConfig.getSites().stream()
                .map(e -> siteRepository.countByNameAndStatus(e.getName(), Status.INDEXING))
                .reduce(0, Integer::sum) > 0) {
            return false;
        }
        parsePage.clearUniqueLinks();

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Cайт: %d")
                .build();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, threadFactory);
        executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());

        siteListFromConfig.getSites().forEach(e -> {
            if (parsePage.isCancelled()) {
                executor.shutdownNow();
            } else {
                executor.execute(() -> indexingPage(e.getUrl()));
            }
        });

        executor.shutdown();
        return true;
    }


    void deleteByName(String name) {
        Optional<SiteE> siteByName = siteRepository.findByName(name);
        if (siteByName.isPresent()) {
            int siteId = siteByName.get().getSiteId();

            log.warn("lemma deleteAllBySiteId: {}", siteId);
            try {
                lemmaRepository.deleteAllBySiteId(siteId);
            } catch (Exception e) {
                log.error("lemmaRepository.deleteAllBySiteIdInBatch() message: {}", e.getMessage());
            }
            log.warn("page deleteAllBySiteId: {}", siteId);
            try {
                pageRepository.deleteAllBySiteId(siteId);
            } catch (Exception e) {
                log.error("pageRepository.deleteAllBySiteIdInBatch() message: {}", e.getMessage());
            }

        }
    }

    @Transactional
    void parsingOneSite(String url, String name, boolean isCreate) {
        parsePage.setCancelled(new AtomicBoolean(false));
        SiteE siteE;
        int siteId;
        System.out.println();
        if (isCreate) {
            siteE = new SiteE(Status.INDEXING, Utils.setNow(), url, name);
            log.info("<<<=== Site '{}' added", name);
        } else {
            siteE = siteRepository.findByName(name).orElse(null);
            if (siteE == null) {
                log.warn("Сайт {} не найден", name);
                return;
            }
            siteE.setStatus(Status.INDEXING);

            log.info("<<<=== Site '{}' changed", siteE.getName());
            deleteByName(name);
        }

        siteE = siteRepository.save(siteE);
        siteId = siteE.getSiteId();
        siteEList.add(siteE);

        // подготовка данных для
        siteParser.initSiteParser(siteId, Utils.getProtocolAndDomain(url), url);

        // вызов парсинга сайтов
        siteParser.getLinks();
    }

    //  Метод останавливает текущий процесс индексации (переиндексации).
    //  Если в настоящий момент индексация или переиндексация не происходит,
    //  метод возвращает соответствующее сообщение об ошибке.
    @Override
    //@Transactional
    public Object stopIndexing() {
        if (stopping()) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError(Messages.INDEXING_IS_NOT_RUNNING);
            response = responseFalse;
        }
        return response;
    }

    private boolean stopping() {

        try {
            long size = siteEList.stream().filter(e -> e.getStatus() == Status.INDEXING).count();
            if (size == 0) {
                log.warn(Messages.INDEXING_IS_NOT_RUNNING);
                return false;
            }
            parsePage.setCancelled(new AtomicBoolean(true));

            //SiteParser.forceStop();
            siteParser.forceStop();

            executor.shutdownNow();

            siteEList.stream()
                    .filter(e -> e.getStatus() == Status.INDEXING)
                    .forEach(e -> {
                        e.setStatus(Status.FAILED);
                        e.setStatusTime(new Timestamp(System.currentTimeMillis()));
                        e.setLastError(Messages.INDEXING_STOPPED_BY_USER);
                    });
            siteRepository.saveAll(siteEList);

            System.out.println("===> " + Thread.currentThread());
            log.warn(Messages.INDEXING_STOPPED_BY_USER);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }

        return true;
    }

    //  Метод добавляет в индекс или обновляет отдельную страницу,
    //  адрес которой передан в параметре.
    //  Если адрес страницы передан неверно,
    //  метод должен вернуть соответствующую ошибку.
    //
    // url — адрес страницы, которую нужно переиндексировать.
    //@Transactional
    @Override
    public Object indexPage(String url) {
        if (indexingPage(url)) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError(Messages.THIS_PAGE_IS_LOCATED_OUTSIDE_THE_SITES_SPECIFIED_IN_THE_CONFIGURATION_FILE);
            response = responseFalse;
        }
        return response;
    }

    private boolean indexingPage(String url) {
        if (siteListFromConfig.getSites().stream().noneMatch(site -> site.getUrl().equals(url))) {
            return false;
        }
        Site site = siteListFromConfig.getSites().stream()
                .filter(s -> s.getUrl().equals(url))
                .findFirst()
                .orElse(null);
        if (site == null) {
            log.warn("site == null");
            return false;
        }
        String name = site.getName();
        parsePage.clearUniqueLinks();
        parsingOneSite(url, name, siteRepository.findByName(name).isEmpty());

        return true;
    }
}
