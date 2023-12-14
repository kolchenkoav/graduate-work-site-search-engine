package searchengine.services;

public interface IndexingService {
    /**
     * Метод запускает полную индексацию всех сайтов
     * или полную переиндексацию, если они уже проиндексированы.
     * Если в настоящий момент индексация или переиндексация уже запущена,
     * метод возвращает соответствующее сообщение об ошибке.
     * @see <a href="https://github.com/kolchenkoav/graduate-work-site-search-engine/blob/master/docs/startIndexing.md">Запуск полной индексации</a>
     */
    Object startIndexing();

    /**
     * Метод останавливает текущий процесс индексации (переиндексации).
     * Если в настоящий момент индексация или переиндексация не происходит,
     * метод возвращает соответствующее сообщение об ошибке.
     * @see <a href="https://github.com/kolchenkoav/graduate-work-site-search-engine/blob/master/docs/startIndexing.md">Запуск полной индексации</a>
     */
    Object stopIndexing();
    Object indexPage(String url);
}
