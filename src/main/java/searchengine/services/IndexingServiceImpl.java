package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.controllers.DefaultController;
import searchengine.dto.indexing.IndexingErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.lemma.LemmaFinder;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
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
            responseFalse.setError("Индексация уже запущена");
            response = responseFalse;
        }
        controller.index();
        return response;
    }


    private boolean indexing() {
        boolean isResultOk = true;

        List<Site> sitesList = sites.getSites();
        if (sitesList.stream()
                .map(e -> siteRepository.countByNameAndStatus(e.getName(), Status.INDEXING))
                .reduce(0, Integer::sum) > 0) {
            isResultOk = false;
        } else {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
            sitesList.forEach(e -> {
                //SiteParser sp = siteParser.copy();

                String name = e.getName();
                Optional<SiteE> siteByName = siteRepository.findByName(name);
                if (siteByName.isPresent()) {
                    log.warn("deleteAllByName: " + name);
                    siteRepository.deleteAllByName(name);
                }

                SiteE siteE = new SiteE(Status.INDEXING, new Timestamp(System.currentTimeMillis()), e.getUrl(), e.getName());
                //siteTList.add(siteE);
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
                });
            });
        }

        return isResultOk;
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
            responseFalse.setError("Индексация не запущена");
            response = responseFalse;
        }
        return response;
    }

    // TODO Остановка текущей индексации
    private boolean stopping() {
        // Тут написать код

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
        if (indexingPage()) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError("Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле");
            response = responseFalse;
        }
        return response;
    }

    // TODO Добавление или обновление отдельной страницы
    private boolean indexingPage() {
        // Тут написать код

        return false;
    }

}
