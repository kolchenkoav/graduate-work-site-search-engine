package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import searchengine.config.Messages;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchErrorResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.lemma.LemmaFinder;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final SiteList sites;

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
        log.info("search => query: '{}'  site: '{}' offset: {} limit: {}", query, site, offset, limit);

        List<Site> siteList;
        if (site == null) {
            // поиск по всем сайтам в списке
            siteList = sites.getSites();
        } else {
            siteList = sites.getSites()
                    .stream()
                    .filter(site1 -> site1.getUrl().equals(site)).toList();
        }



        //  1.  Разбить поисковый запрос на отдельные слова и формировать из этих слов список уникальных лемм
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        assert lemmaFinder != null;
        Map<String, Integer> mapLemmas = lemmaFinder.collectLemmas(query);

        //========================================================
        //  Список сайтов:          List<Site> siteList
        //  Запрос:                 String query
        //  сдвиг:                  0
        //  количество результатов: 20
        //  Запрос список:          Map<String, Integer> mapLemmas
        //========================================================
        siteList.forEach(siteForSearch -> {
            log.info("Поиск по сайту: {} ...", siteForSearch.getUrl());


            });

            //  2.  Исключать из полученного списка леммы, которые встречаются на слишком большом количестве страниц.
            //  Поэкспериментируйте и определите этот процент самостоятельно.

            //  3.  Сортировать леммы в порядке увеличения частоты встречаемости
            //  (по возрастанию значения поля frequency) — от самых редких до самых частых.

            //  4.  По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается.
            //  Далее искать соответствия следующей леммы из этого списка страниц,
            //  а затем повторять операцию по каждой следующей лемме.
            //  Список страниц при этом на каждой итерации должен уменьшаться

            //  5.  Если в итоге не осталось ни одной страницы, то выводить пустой список.


            //  6.  Если страницы найдены, рассчитывать по каждой из них релевантность
            //  (и выводить её потом, см. ниже) и возвращать.


            //  7.  Для каждой страницы рассчитывать абсолютную релевантность — сумму всех rank всех найденных
            //  на странице лемм (из таблицы index), которая делится на максимальное значение
            //  этой абсолютной релевантности для всех найденных страниц.


            //  8.  Сортировать страницы по убыванию релевантности (от большей к меньшей)
            //  и выдавать в виде списка объектов со следующими полями:
            //
            // uri — путь к странице вида /path/to/page/6784;
            // title — заголовок страницы;
            // snippet — фрагмент текста, в котором найдены совпадения (см. ниже);
            // relevance — релевантность страницы (см. выше формулу расчёта).


            //  9.  Сниппеты — фрагменты текстов, в которых найдены совпадения, для всех страниц должны быть
            //  примерно одинаковой длины — такие, чтобы на странице
            //  с результатами поиска они занимали примерно три строки.
            //  В них необходимо выделять жирным совпадения с исходным поисковым запросом.
            //  Выделение должно происходить в формате HTML при помощи тега <b>.
            //  Алгоритм получения сниппета из веб-страницы реализуйте самостоятельно.





        if (isExistsInList(site)) {

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
            responseFalse.setError(Messages.EMPTY_SEARCH_QUERY_SPECIFIED);
            response = responseFalse;
        }
        return response;
    }

    private boolean isExistsInList(String site) {
        List<Site> siteList = sites.getSites();
        return siteList.stream().anyMatch(s -> Objects.equals(s.getUrl(), site));
    }
}
