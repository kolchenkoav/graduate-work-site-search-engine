package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Messages;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.controllers.DefaultController;
import searchengine.dto.indexing.IndexingErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
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
    private final IndexRepository indexRepository;
    private final SiteParser siteParser;
    private final ParsePage parsePage;
    private final SiteList siteListFromConfig;
    private final List<SiteE> siteEList = new ArrayList<>();
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final DefaultController controller;
    private Object response;
    private ThreadPoolExecutor executor;

    /******************************************************************************************
     * Запуск полной индексации
     *
     * @return response (Успешно или ошибка)
     */
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

    /**
     * Индексация по списку из конфигурации
     *
     * @return true -Успешно, false -ошибка
     */
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

    /**
     * Парсинг заданного сайта
     *
     * @param url      ссылка на сайт
     * @param name     имя сайта
     * @param isCreate true -новая запись, false - изменить запись
     */
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

        /** подготовка данных */
        siteParser.initSiteParser(siteId, Utils.getProtocolAndDomain(url), url);

        /** вызов парсинга сайта */
        siteParser.getLinks();
    }

    /**
     * Удаление страниц и лемм по name
     *
     * @param name имя сайта
     */
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

    /******************************************************************************************
     * Метод останавливает текущий процесс индексации
     *
     * @return response (Успешно или ошибка)
     */
    @Override
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

    /**
     * Остановка выполнения индексации
     *
     * @return true -Успешно, false -ошибка
     */
    private boolean stopping() {
        try {
            long size = siteEList.stream().filter(e -> e.getStatus() == Status.INDEXING).count();
            if (size == 0) {
                log.warn(Messages.INDEXING_IS_NOT_RUNNING);
                return false;
            }
            parsePage.setCancelled(new AtomicBoolean(true));

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

    /******************************************************************************************
     * Добавление или обновление отдельной страницы
     *
     * @param url адрес страницы, которую нужно переиндексировать.
     * @return response (Успешно или ошибка)
     */
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

    /**
     * Индексация отдельной страницы
     *
     * @return true -Успешно, false -ошибка
     */
    private boolean indexingPage(String url) {
        String domain = Utils.getProtocolAndDomain(url);

        if (siteListFromConfig.getSites().stream().noneMatch(site -> site.getUrl().equals(domain))) {
            return false;
        }
        Site site = siteListFromConfig.getSites().stream()
                .filter(s -> s.getUrl().equals(domain))
                .findFirst()
                .orElse(null);
        if (site == null) {
            log.warn("site == null");
            return false;
        }

        String name = site.getName();


        SiteE siteE = siteRepository.findByName(name).orElse(null);
        if (siteE == null) {
            siteE = new SiteE(Status.INDEXING, Utils.setNow(), domain, name);
        } else {
            siteE.setStatus(Status.INDEXING);
            siteE.setStatusTime(Utils.setNow());

            String path = url.substring(domain.length());
            deletePage(siteE.getSiteId(), path);
        }
        siteE.setLastError("");
        siteRepository.save(siteE);

        Document doc = parsePage.getDocumentByUrl(url);
        parsePage.setSiteId(siteE.getSiteId());
        parsePage.setDomain(domain);
        parsePage.setUrl(url);

        Page page = parsePage.savePage(doc);
        if (page == null) {
            return false;
        }
        siteParser.parseSinglePage(page);

        siteE.setStatus(Status.INDEXED);
        siteE.setStatusTime(Utils.setNow());
        siteRepository.save(siteE);
        log.info("page saved...");
        return true;
    }

    private void deletePage(int siteId, String path) {
        log.info("The page {} by sideId: {} is deleted", path, siteId);
        Page page = pageRepository.findBySiteIdAndPath(siteId, path);
        if (page != null) {
            deleteLemmas(page, siteId);
            pageRepository.delete(page);
        }
    }

    private void deleteLemmas(Page page, int siteId) {
        List<IndexE> indexList = indexRepository.findByPageId(page.getPageId());
        List<Lemma> lemmaList = new ArrayList<>();
        indexList.forEach(e -> {
                    Lemma lemma = lemmaRepository.findByLemmaId(e.getLemmaId());
                    lemma.setFrequency(lemma.getFrequency() - 1);
                    lemmaList.add(lemma);
                }
        );
        lemmaRepository.saveAll(lemmaList);
        log.info("Lemmas by pageId: {} are removed", page.getPageId());
        lemmaRepository.deleteBySiteIdAndFrequency(siteId, 0);
    }
}
