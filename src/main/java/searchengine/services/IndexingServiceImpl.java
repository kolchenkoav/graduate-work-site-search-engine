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
import searchengine.parsing.SiteParser;
import searchengine.parsing.Utils;
import searchengine.repository.SiteRepository;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    //private final SiteParser siteParser;
    private final SiteList siteListFromConfig;
    private final List<SiteE> siteEList = new ArrayList<>();
    private final SiteRepository siteRepository;
    private final DefaultController controller;
    private Object response;

    //  Метод запускает полную индексацию всех сайтов
    //  или полную переиндексацию, если они уже проиндексированы.
    //  Если в настоящий момент индексация или переиндексация уже запущена,
    //  метод возвращает соответствующее сообщение об ошибке.
    @Transactional
    @Override
    public Object startIndexing() {

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

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Парсинг сайтов: %d")
                .build();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4, threadFactory);
        executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());

        siteListFromConfig.getSites().forEach(e -> {
            deleteByName(e.getName());
            executor.execute(() -> parsingOneSite(e.getUrl(), e.getName()));
        });
        return true;
    }

    private void deleteByName(String name) {
        Optional<SiteE> siteByName = siteRepository.findByName(name);
        if (siteByName.isPresent()) {
            log.warn("deleteAllByName: " + name);
            siteRepository.deleteAllByName(name);
        }
    }

    private void parsingOneSite(String url, String name) {
        SiteE siteE = new SiteE(Status.INDEXING, new Timestamp(System.currentTimeMillis()), url, name);
        siteEList.add(siteE);

        siteE.setStatus(Status.INDEXING);
        siteRepository.save(siteE);

        //TODO вызов парсинга сайтов
        log.info("Parse => url: {} name: {}", url, name);

        SiteParser siteParser = new SiteParser();
        siteParser.setSiteId(siteE.getSiteId());
        siteParser.setUrl(url);
        siteParser.setDomain(Utils.getProtocolAndDomain(url));

        siteParser.getLinks();
//        siteParser.setUrl(url);
//        siteParser.getLinks();


        siteE.setStatus(Status.INDEXED);
        siteE.setStatusTime(new Timestamp(System.currentTimeMillis()));
        siteRepository.save(siteE);
    }

    //  Метод останавливает текущий процесс индексации (переиндексации).
    //  Если в настоящий момент индексация или переиндексация не происходит,
    //  метод возвращает соответствующее сообщение об ошибке.
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


    private boolean stopping() {
        try {
            long size = siteEList.stream().filter(e -> e.getStatus() == Status.INDEXING).count();
            if (size == 0) {
                log.warn(Messages.INDEXING_IS_NOT_RUNNING);
                return false;
            }

            //SiteParser.forceStop();
            //public static void forceStop() {
            //        if (poolList != null && !poolList.isEmpty()) {
            //            poolList.forEach(ForkJoinPool::shutdownNow);
            //            poolList = new ConcurrentLinkedQueue<>();
            //        }
            //    }

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
    @Transactional
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
            log.warn("");
            return false;
        }
        String name = site.getName();
        deleteByName(name);
        parsingOneSite(url, name);
        return true;
    }

}
