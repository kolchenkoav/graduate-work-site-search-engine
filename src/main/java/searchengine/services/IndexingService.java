package searchengine.services;

public interface IndexingService {
    /**
     * Метод запускает полную индексацию всех сайтов
     * или полную переиндексацию, если они уже проиндексированы.
     * Если в настоящий момент индексация или переиндексация уже запущена,
     * метод возвращает соответствующее сообщение об ошибке.
     *
     * @see <a href="https://github.com/kolchenkoav/graduate-work-site-search-engine/blob/master/docs/startIndexing.md">Запуск полной индексации</a>
     */
    Object startIndexing();

    /**
     * Метод останавливает текущий процесс индексации (переиндексации).
     * Если в настоящий момент индексация или переиндексация не происходит,
     * метод возвращает соответствующее сообщение об ошибке.
     *
     * @see <a href="https://github.com/kolchenkoav/graduate-work-site-search-engine/blob/master/docs/stopindexing.md">Остановка текущей индексации</a>
     */
    Object stopIndexing();

    /**
     * Метод добавляет в индекс или обновляет отдельную страницу
     * адрес которой передан в параметре.
     * Если адрес страницы передан неверно, метод должен вернуть соответствующую ошибку.
     *
     * @param url — адрес страницы, которую нужно переиндексировать.
     * @see <a href="https://github.com/kolchenkoav/graduate-work-site-search-engine/blob/master/docs/indexPage.md">Добавление или обновление отдельной страницы</a>
     */
    Object indexPage(String url);
}
