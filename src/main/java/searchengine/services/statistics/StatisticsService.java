package searchengine.services.statistics;

import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {

    /**
     * Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и самого движка.
     * Если ошибок индексации того или иного сайта нет, задавать ключ error не нужно.
     * @return StatisticsResponse
     */
    StatisticsResponse getStatistics();
}
