package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchErrorResponse;
import searchengine.dto.search.SearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SitesList sites;

    //====================================================================================================
    //  Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
    //  Чтобы выводить результаты порционно, также можно задать параметры offset
    //  (сдвиг от начала списка результатов) и limit (количество результатов, которое необходимо вывести).
    //  В ответе выводится общее количество результатов (count),
    //  не зависящее от значений параметров offset и limit, и массив data с результатами поиска.
    //  Каждый результат — это объект, содержащий свойства результата поиска
    //  (см. ниже структуру и описание каждого свойства).
    //  Если поисковый запрос не задан или ещё нет готового индекса
    //  (сайт, по которому ищем, или все сайты сразу не проиндексированы),
    //  метод должен вернуть соответствующую ошибку (см. ниже пример).
    //  Тексты ошибок должны быть понятными и отражать суть ошибок.
    //
    //  Параметры: String query, (required = false) String site, int offset, (defaultValue = 20) int limit
    //
    //  query — поисковый запрос;
    //  site — сайт, по которому осуществлять поиск (если не задан,
    //      поиск должен происходить по всем проиндексированным сайтам);
    //      задаётся в формате адреса, например: http://www.site.com (без слэша в конце);
    //  offset — сдвиг от 0 для постраничного вывода (параметр необязательный;
    //      если не установлен, то значение по умолчанию равно нулю);
    //  limit — количество результатов, которое необходимо вывести
    //      (параметр необязательный; если не установлен, то значение по умолчанию равно 20).
    @Override
    public Object search(String query, String site, int offset, int limit) {
        Object response;
        List<SearchData> searchDataList = new ArrayList<>();

        if (searching(site)) {
            SearchResponse responseTrue = new SearchResponse();
            responseTrue.setResult(true);
            responseTrue.setCount(2);
            SearchData searchData = new SearchData(site,
                    "Имя сайта",
                    "/path/to/page/6784",
                    "Заголовок страницы, которую выводим",
                    "Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML",
                    0.93362);
            searchDataList.add(searchData);

            searchData = new SearchData(site,
                    "Тоже имя сайта",
                    "/path/to/page/777",
                    "Заголовок страницы, которую выводим ого-го",
                    "Тоже фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML",
                    0.6);
            searchDataList.add(searchData);
            responseTrue.setData(searchDataList);
            response = responseTrue;
        } else {
            SearchErrorResponse responseFalse = new SearchErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError("Задан пустой поисковый запрос");
            response = responseFalse;
        }
        return response;
    }

    // TODO Получение данных по поисковому запросу
    private boolean searching(String site) {
        List<Site> sitesList = sites.getSites();
        return sitesList.stream().anyMatch(s-> Objects.equals(s.getUrl(), site));
    }
}
