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
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final SiteList sites;
    //private String wordSearch;
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    //String content = null;
    private int limitFrequency;
    private double[][] relevance;
    private double maxRelevance;
    private List<SearchResults> searchResultsList;

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
        searchResultsList = new ArrayList<>();
        List<SearchData> searchDataList = new ArrayList<>();
        log.info("search => query: '{}'  site: '{}' offset: {} limit: {}", query, site, offset, limit);

        List<Site> siteList = getSiteList(site);
        if (siteList == null) {
            log.warn("Search site {} not found", site);
            return getResponseFalse();
        }

        Map<String, Integer> mapLemmas = Objects.requireNonNull(lemmaFinder).collectLemmas(query);
        if (mapLemmas == null) {
            log.warn("Lemmas for search not found");
            return getResponseFalse();
        }

        /*
         Список сайтов:          List<Site> siteList
         Запрос:                 String query
         сдвиг:                  0
         количество результатов: 20
         Запрос список:          Map<String, Integer> mapLemmas
         */
        List<Lemma> lemmaList = new ArrayList<>();
        List<Lemma> finalLemmaList = lemmaList;
        limitFrequency = 300; //TODO limit
        AtomicBoolean isSearchWordMissing = new AtomicBoolean(true);
        siteList.forEach(siteForSearch -> {
            SiteE siteE = siteRepository.findByName(siteForSearch.getName()).orElse(null);
            int siteId = 0;
            if (siteE != null) {
                siteId = siteE.getSiteId();
            }

            log.info("Поиск по сайту: {} ...", siteForSearch.getUrl());
            int finalSiteId = siteId;
            mapLemmas.forEach((k, v) -> {
                Lemma lemma = lemmaRepository.findBySiteIdAndLemma(finalSiteId, k).orElse(null);
                if (lemma != null) {
                    isSearchWordMissing.set(false);
                    /* 2. Исключает из списка леммы, которые встречаются на слишком большом количестве страниц */
                    if (lemma.getFrequency() < limitFrequency) {
                        finalLemmaList.add(lemma);
                    }
                } else {
                    log.warn("search word: '{}' not found in database", k);
                }
            });
        });

        if (isSearchWordMissing.get()) {
            return getResponseFalse();
        }

        // 3.
        lemmaList = finalLemmaList.stream().sorted(Comparator.comparingInt(Lemma::getFrequency)).collect(Collectors.toList());
        if (lemmaList.size() == 0) {
            log.warn("lemmaList.size = 0");
            return getResponseFalse();
        }


        List<Integer> list = lemmaList.stream().mapToInt(Lemma::getSiteId).distinct().boxed().toList();
        for (Integer i : list) {
            FormationForOneSite(lemmaList.stream().filter(lemma -> lemma.getSiteId() == i).toList());

            System.out.println();
            System.out.println("========== Fill relevance ========= siteId: " + i);
            System.out.println(Arrays.deepToString(relevance));

            // заполняем
            for (int j = 0; j < relevance.length; j++) {
                SearchResults results;
                for (SearchResults searchResults : searchResultsList) {
                    results = searchResults;
                    int ind = relevance[j].length - 1;
                    if (results.getNumber() == (j + 1) && results.getSiteId() == i) {
                        results.setRelevance(relevance[j][ind]);
                    }
                }
            }
        }

        //  8.  Сортировать страницы по убыванию релевантности (от большей к меньшей)
        //  и выдавать в виде списка объектов со следующими полями:
        //
        // uri — путь к странице вида /path/to/page/6784;
        // title — заголовок страницы;
        // snippet — фрагмент текста, в котором найдены совпадения (см. ниже);
        // relevance — релевантность страницы (см. выше формулу расчёта).
        searchResultsList = searchResultsList.stream()
                .sorted(Comparator
                        .comparing(SearchResults::getRelevance)
                        .reversed())
                .toList();




        //  9.  Сниппеты — фрагменты текстов, в которых найдены совпадения, для всех страниц должны быть
        //  примерно одинаковой длины — такие, чтобы на странице
        //  с результатами поиска они занимали примерно три строки.
        //  В них необходимо выделять жирным совпадения с исходным поисковым запросом.
        //  Выделение должно происходить в формате HTML при помощи тега <b>.
        //  Алгоритм получения сниппета из веб-страницы реализуйте самостоятельно.

        Iterator<SearchResults> iterator = searchResultsList.iterator();
        SearchResults results;
        while (iterator.hasNext()) {
            results = iterator.next();
            Page page = pageRepository.findByPageId(results.getPageId());
            results.setTitle(page.getTitle());
            results.setUrl(page.getPath());

            String snippet = getSnippet(page.getContent(), lemmaList.stream()
                    .map(Lemma::getLemma)
                    .collect(Collectors.toList()));

            results.setSnippet(snippet);
        }

        System.out.println();
        System.out.println("searchResultsList");
        searchResultsList.forEach(System.out::println);


        Object response;


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
            response = getResponseFalse();

        }

        return response;
    }

    private void FormationForOneSite(List<Lemma> lemmaList) {
        System.out.println();
        System.out.println("=> lemmaList:");
        lemmaList.forEach(System.out::println);


        // 4. По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается
        List<IndexE> indexList = new ArrayList<>(Objects
                .requireNonNull(indexRepository.findByLemmaId(lemmaList.get(0).getLemmaId())
                        .orElse(null)));
        List<Page> pageList = new ArrayList<>();
        for (IndexE indexE : indexList) {
            Page page = pageRepository.findByPageId(indexE.getPageId());
            if (page != null) {
                pageList.add(page);
            }
        }
        System.out.println("=> pageList:");
        pageList.forEach(System.out::println);


        //          lem1 lem2
        //      0   1    2    3    4        K -кол-во лемм
        //  0   [1] [r1] [r2] [ar] [or]
        //  1   [2] []   []   [ar] [or]
        //
        //  J -кол-во страниц (индексов)
        // Заполняем № и колонку первой леммы
        relevance = new double[indexList.size()][lemmaList.size() + 3];
        for (int j = 0; j < indexList.size(); j++) {
            for (int k = 0; k < 2; k++) {    //
                if (k == 0) {
                    relevance[j][k] = j + 1;
                    SearchResults searchResults = new SearchResults();
                    searchResults.setNumber(j + 1);
                    searchResults.setSiteId(lemmaList.get(0).getSiteId());
                    searchResults.setPageId(indexList.get(j).getPageId());
                    searchResultsList.add(searchResults);
                    continue;
                }
                relevance[j][k] = indexList.get(j).getRank();
            }
        }

        // Поиск соответствия леммы из списка страниц
        // И заполнение колонок следующих лемм
        int i = 1;
        while (i < lemmaList.size()) {
            int lemmaId = lemmaList.get(i).getLemmaId();
            //System.out.println("*** " + lemmaId);
            List<IndexE> indexList2 = new ArrayList<>(Objects.requireNonNull(indexRepository.findByLemmaId(lemmaId).orElse(null)));

            if (!pageList.removeIf(page -> indexList2.stream()
                    .noneMatch(indexE -> indexE.getPageId() == page.getPageId()))) {
                // удаляем лишние индексы
                indexList2.removeIf(indexE -> pageList.stream().noneMatch(page -> page.getPageId() == indexE.getPageId()));
                //indexList2.forEach(System.out::println);
                for (int j = 0; j < indexList2.size(); j++) {
                    relevance[j][i + 1] = indexList2.get(j).getRank();
                }
            }
            i++;
        }

        setAbsoluteRelevance(pageList, lemmaList);
        setRelativeRelevance(pageList, lemmaList);

//        System.out.println();
//        System.out.println("========== Fill relevance =========");
//        System.out.println(Arrays.deepToString(relevance));

        System.out.println("=> после прохода pageList:");
        pageList.forEach(System.out::println);
    }

    private void setRelativeRelevance(List<Page> pageList, List<Lemma> lemmaList) {
        for (int j = 0; j < pageList.size(); j++) {
            int ind = lemmaList.size() + 2;
            relevance[j][ind] = relevance[j][ind - 1] / maxRelevance;
        }
    }

    private void setAbsoluteRelevance(List<Page> pageList, List<Lemma> lemmaList) {
        maxRelevance = 0;
        for (int j = 0; j < pageList.size(); j++) {
            for (int k = 0; k < lemmaList.size() + 3; k++) {
                if (k == lemmaList.size() + 1) {
                    int sumAR = 0;
                    for (int l = 0; l < lemmaList.size(); l++) {
                        sumAR += relevance[j][l + 1];
                    }
                    relevance[j][k] = sumAR;
                    maxRelevance = Double.max(maxRelevance, sumAR);
                }
            }
        }
    }

    /**
     * Исключает из списка леммы, которые встречаются на слишком большом количестве страниц.
     * Поэкспериментируйте и определите этот процент самостоятельно.
     *
     * @param lemmaList список лемм
     * @return обработанный список лемм
     */
    private List<Lemma> ExcludeLemmasFromTheResultingList(List<Lemma> lemmaList) {
        //TODO limitFrequency определить
        limitFrequency = 3;
        return lemmaList.stream()
                .filter(lemma -> lemma.getFrequency() < limitFrequency)
                .collect(Collectors.toList());
    }

    private Object getResponseFalse() {
        SearchErrorResponse responseFalse = new SearchErrorResponse();
        responseFalse.setResult(false);
        responseFalse.setError(Messages.EMPTY_SEARCH_QUERY_SPECIFIED);
        return responseFalse;
    }

    private List<Site> getSiteList(String site) {
        List<Site> siteList;
        if (site == null) {
            // поиск по всем сайтам в списке
            siteList = sites.getSites();
        } else {
            siteList = sites.getSites()
                    .stream()
                    .filter(site1 -> site1.getUrl().equals(site)).toList();
        }
        return siteList;
    }

    private List<String> getList(String wordSearch, String content) {
        //String wordStartsWith = wordSearch.substring(0, (wordSearch.length() / 2) + 1).toLowerCase(Locale.ROOT);
        log.info("wordSearch: {}", wordSearch);

        //content = getContentFrom();
        List<String> list = Arrays.stream(content.split(" "))
                //.filter(f -> f.toLowerCase(Locale.ROOT).startsWith(wordStartsWith))
                .filter(f -> getOneLemma(f, wordSearch))
                .findFirst().stream().toList();
        log.info("list.size: {}", list.size());
        return list;
    }

    private String getSnippet(String content, String s) {
        int index = content.indexOf(s);
        int beginIndex;
        int endIndex;

        beginIndex = Math.max(index - 100, 0);
        endIndex = Math.min(index + 100, content.length());
        log.info("index: {} beginIndex: {} endIndex: {}", index, beginIndex, endIndex);
        return content.substring(beginIndex, endIndex).replace(s, "<b>" + s + "</b>");
    }

    private String getSnippet(String content, List<String> list) {
        String result = content;
        for (String s: list) {
            List<String> list1 = getList(s, content);
            result = getSnippet(result, s);
        }
        return result;
    }

    private boolean getOneLemma(String f, String wordSearch) {
        boolean bool;

        Map<String, Integer> lemma1 = Objects.requireNonNull(lemmaFinder).collectLemmas(f);
        Map<String, Integer> lemma2 = Objects.requireNonNull(lemmaFinder).collectLemmas(wordSearch);
        String s1 = lemma1.keySet().toString();
        String s2 = lemma2.keySet().toString();
        bool = s1.equals(s2);
        //log.info("lemma1: {} lemma2: {} s1: {} s2: {}", lemma1, lemma2, s1, s2);
        System.out.print(".");
        if (bool) {
            log.info("====>>>> lemma1: {} lemma2: {} s1: {} s2: {}", lemma1, lemma2, s1, s2);
        }
        return bool;
    }


    private boolean isExistsInList(String site) {
        List<Site> siteList = sites.getSites();
        return siteList.stream().anyMatch(s -> Objects.equals(s.getUrl(), site));
    }
}
