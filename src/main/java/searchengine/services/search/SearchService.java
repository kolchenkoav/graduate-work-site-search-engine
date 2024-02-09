package searchengine.services.search;

import searchengine.dto.Response;

public interface SearchService {
    /**
     * Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
     *
     * @param query  — поисковый запрос;
     * @param site   — сайт, по которому осуществлять поиск (если не задан,
     *               поиск должен происходить по всем проиндексированным сайтам);
     *               задаётся в формате адреса, например: http://www.site.com (без слэша в конце);
     * @param offset — сдвиг от 0 для постраничного вывода (параметр необязательный;
     *               если не установлен, то значение по умолчанию равно нулю);
     * @param limit  — количество результатов, которое необходимо вывести
     *               (параметр необязательный; если не установлен, то значение по умолчанию равно 20).
     * @return response
     */
    Response search(String query, String site, int offset, int limit);
}
