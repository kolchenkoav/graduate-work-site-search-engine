package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingErrorResponse;
import searchengine.dto.indexing.IndexingResponse;


@Service
public class IndexingServiceImpl implements IndexingService {
    private Object response;

    //  Метод запускает полную индексацию всех сайтов
    //  или полную переиндексацию, если они уже проиндексированы.
    //  Если в настоящий момент индексация или переиндексация уже запущена,
    //  метод возвращает соответствующее сообщение об ошибке.
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
        return response;
    }

    // TODO Запуск полной индексации
    private boolean indexing() {
        // Тут написать код

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
