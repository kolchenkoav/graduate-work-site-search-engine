package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Messages;
import searchengine.config.SiteList;
import searchengine.controllers.DefaultController;
import searchengine.dto.indexing.IndexingErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteE;
import searchengine.model.Status;
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
            //SiteParser sp = siteParser.copy();

            String name = e.getName();
            Optional<SiteE> siteByName = siteRepository.findByName(name);
            if (siteByName.isPresent()) {
                log.warn("deleteAllByName: " + name);
                siteRepository.deleteAllByName(name);
            }

            SiteE siteE = new SiteE(Status.INDEXING, new Timestamp(System.currentTimeMillis()), e.getUrl(), e.getName());
            siteEList.add(siteE);

            //sp.init(siteT, 3);

            executor.execute(() -> {
                siteE.setStatus(Status.INDEXING);
                siteRepository.save(siteE);
                log.info("Save INDEXING " + name);

                for (int i = 0; i < 10; i++) {
                    System.out.println(name + " i: " + i);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                System.out.println();
                siteE.setStatus(Status.INDEXED);
                siteRepository.save(siteE);
                log.info("*** Save INDEXED " + name);
                System.out.println("Thread.currentThread(): " + Thread.currentThread().getName());
            });
        });
        return true;
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
        if (!siteListFromConfig.getSites().stream().anyMatch(site -> site.getUrl().equals(url))) {
            return false;
        }
        // TODO Добавление или обновление отдельной страницы
        log.info("Добавление или обновление отдельной страницы " + url);
        return true;
    }

}
