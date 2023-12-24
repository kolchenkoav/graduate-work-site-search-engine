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
            /*
         сдвиг:                     offset
         количество результатов:    limit
         Список Id сайтов:          List<Integer> siteIdList
         Лемма список из запроса:   List<String> lemmaListFromQuery
         Лемма из БД:               List<Lemma> lemmaList
         */
    @Override
    public Object search(String query, String site, int offset, int limit) {
        log.info("search => query: '{}'  site: '{}' offset: {} limit: {}", query, site, offset, limit);

        List<Integer> siteIdList = getSiteIdList(site);
        if (siteIdList == null) {
            log.warn("Search site {} not found", site);
            return getResponseFalse();
        }

        List<String> lemmaListFromQuery = Objects.requireNonNull(lemmaFinder).collectLemmas(query)
                .keySet()
                .stream()
                .toList();
        if (lemmaListFromQuery == null) {
            log.warn("Lemmas from query:'{}' not found", query);
            return getResponseFalse();
        }

        searchResultsList = new ArrayList<>();
        List<SearchData> searchDataList = new ArrayList<>();


        List<Lemma> lemmaList = new ArrayList<>();
        for (Integer siteId : siteIdList) {
            for (String lem : lemmaListFromQuery) {
                lemmaRepository.findBySiteIdAndLemma(siteId, lem).ifPresent(lemmaList::add);
            }
        }
        if (lemmaList.size() == 0) {
            log.warn("search lemmas: '{}' not found in database", lemmaListFromQuery
                    .stream()
                    .map(s -> s + " ")
                    .collect(Collectors.joining()));
            return getResponseFalse();
        }

        /* 2. Исключает из списка леммы, которые встречаются на слишком большом количестве страниц */
        Iterator<Lemma> iterator = lemmaList.iterator();
        while (iterator.hasNext()) {
            Lemma lemma = iterator.next();
            int countPages = pageRepository.countBySiteId(lemma.getSiteId());

            limitFrequency = countPages;
            log.info("siteId: {} countPages: {} Frequency: {}", lemma.getSiteId(), countPages, lemma.getFrequency());
            if (lemma.getFrequency() >= limitFrequency && countPages > 10) {
                iterator.remove();
            }
        }
        if (lemmaList.size() == 0) {
            log.warn("lemmaList.size = 0");
            return getResponseFalse();
        }

        // siteIdList получает id из lemmaList
        siteIdList = lemmaList.stream().map(Lemma::getSiteId).distinct().toList();

        // 3 Сортировка
        lemmaList = lemmaList.stream().sorted(Comparator.comparingInt(Lemma::getFrequency)).collect(Collectors.toList());

        lemmaList.forEach(System.out::println);


        //List<Integer> list = lemmaList.stream().mapToInt(Lemma::getSiteId).distinct().boxed().toList();
        for (Integer i : siteIdList) {
            //System.out.println("i: " + i);
            //log.info("lemmaList.size: {} ", String.valueOf(lemmaList.stream().filter(lemma -> lemma.getSiteId() == i).toList().size()));
            FormationForOneSite(lemmaList.stream().filter(lemma -> lemma.getSiteId() == i).toList());

            System.out.println();
            System.out.println("========== Fill relevance ========= siteId: " + i);
            System.out.println(Arrays.deepToString(relevance));

            // заполняем searchResultsList
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

        System.out.println("* lemmaList");
        lemmaList.forEach(System.out::println);

        System.out.println("** lemmaListFromQuery");
        lemmaListFromQuery.forEach(System.out::println);
        System.out.println();
        System.out.println("*******************************************");


        Iterator<SearchResults> iteratorSR = searchResultsList.iterator();
        SearchResults results;
        while (iteratorSR.hasNext()) {
            results = iteratorSR.next();
            Page page = pageRepository.findByPageId(results.getPageId());
            results.setTitle(page.getTitle());
            results.setUrl(page.getPath());

            String snippet = getSnippet(page, lemmaList);

            results.setSnippet(snippet);
        }

        System.out.println();
        System.out.println("searchResultsList");
        searchResultsList.forEach(System.out::println);

//        if (true) {
//            return getResponseFalse();
//        }
        Object response;
        response = getResponseFalse();

        if (searchResultsList.size() == 0) {
            return response;
        }
        SearchResponse responseTrue = new SearchResponse();

        responseTrue.setResult(true);
        responseTrue.setCount(searchResultsList.size());
        for (int i = 0; i < searchResultsList.size(); i++) {
            SiteE siteE = siteRepository.getSiteEBySiteId(searchResultsList.get(i).getSiteId());
            String url = siteE.getUrl()+searchResultsList.get(i).getUrl();
            System.out.println("===> url: " + url);
            SearchData searchData = new SearchData(site,
                    siteE.getName(),
                    url,
                    searchResultsList.get(i).getTitle(),
                    searchResultsList.get(i).getSnippet(),
                    searchResultsList.get(i).getRelevance());
            searchDataList.add(searchData);
        }
        responseTrue.setData(searchDataList);
        response = responseTrue;
        return response;
    }

    /**
     * Получяет список Id сайтов из конфигурации которые есть в БД
     *
     * @param site if null - all sites
     * @return list of siteIds
     */
    private List<Integer> getSiteIdList(String site) {
        List<Integer> siteIdList = new ArrayList<>();
        if (site == null) {
            List<Site> siteList = sites.getSites();
            siteIdList = siteList.stream()
                    .map(Site::getName)
                    .map(s -> {
                        if (siteRepository.existsByName(s)) {
                            return siteRepository.findSiteEByName(s).get(0).getSiteId();
                        } else {
                            return 0;
                        }
                    }).toList();
        } else {
            Optional<Site> siteFromConfig = sites.getSites().stream()
                    .filter(site1 -> site1.getUrl().equals(site)).findFirst();
            if (siteFromConfig.isPresent()) {
                SiteE siteE = siteRepository.findByName(siteFromConfig.get().getName()).orElse(null);
                if (siteE != null) {
                    siteIdList.add(siteE.getSiteId());
                }
            }
        }
        return siteIdList.stream().filter(integer -> integer != 0).toList();
    }

    /**
     * Формирует таблицу relevance
     * Определяет релевантность
     *
     * @param lemmaList список лемм для поиска в IndexE и Page
     */
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
            List<IndexE> indexList2 = new ArrayList<>(Objects.requireNonNull(indexRepository.findByLemmaId(lemmaId).orElse(null)));

            if (!pageList.removeIf(page -> indexList2.stream()
                    .noneMatch(indexE -> indexE.getPageId() == page.getPageId()))) {
                // удаляем лишние индексы
                indexList2.removeIf(indexE -> pageList.stream().noneMatch(page -> page.getPageId() == indexE.getPageId()));
                for (int j = 0; j < indexList2.size(); j++) {
                    relevance[j][i + 1] = indexList2.get(j).getRank();
                }
            }
            i++;
        }

        setAbsoluteRelevance(pageList, lemmaList);
        setRelativeRelevance(pageList, lemmaList);

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

    private String getSnippet2(String content, List<Lemma> lemmaList) {
        log.info("*** content: {}", content);

        long startTime = System.currentTimeMillis();

        List<String> splitContent = Arrays.stream(content
                        .toLowerCase(Locale.ROOT)
                .trim()
                .split("\\s+"))
                //.parallel()
                .toList();

        Map<String, Boolean> mapFoundWords = new HashMap<>();
        for (Lemma lemma: lemmaList) {
            mapFoundWords.put(lemma.getLemma(), false);
        }
        //mapFoundWords.forEach((k, v) -> log.info("k: {} v:{}", k, v));

        Map<String, Integer> listPosition = new HashMap<>();
        boolean b = false;
        for (int i = 0; i < splitContent.size(); i++) {
            String word = splitContent.get(i);


            String regex = "[^A-Za-zА-Яа-я0-9]";
            word = word.replaceAll(regex, " ").trim();
            String[] sw = word.split(" ");
            if (sw.length > 1) {
                word = sw[0];
            }

            String wordLemma = Objects.requireNonNull(lemmaFinder).getLemma(word);

            for (Lemma lemma: lemmaList) {
                if (lemma.getLemma().equalsIgnoreCase(wordLemma)) {
                    listPosition.put(wordLemma, i);
                    mapFoundWords.put(wordLemma, true);
                    //log.info("word: {}   wordLemma: {} i: {}", word, wordLemma, i);

                    for (Boolean bool: mapFoundWords.values()) {
                        b = bool;
                    }
                }
            }
//            boolean b1 = false;
//            if (listPosition.size() > 0) {
//                int prevPosition = listPosition.values().stream().toList().get(0);
//                for (int j = 1; j < listPosition.size(); j++) {
//                    b1 = listPosition.values().stream().toList().get(j) - prevPosition < 3;
//                    System.out.println("b1: " + b1);
//                }
//            }
            if (b) { //&& b1 && mapFoundWords.size() == lemmaList.size()
                break;
            }
        }

        mapFoundWords.forEach((k, v) -> log.info("mapFoundWords : k: {} v:{}", k, v));
        System.out.println();
        listPosition.forEach((k, v) -> log.info("listPosition: k: {} v:{}", k, v));

        //  Get snippet from pageId: 10
        //  *** content: Главное Россия Мир Бывший СССР Экономика Силовые структуры Наука и техни
        //  listPosition: k: цена v:55
        //  listPosition: k: яйцо v:57
        String snippet = "";
        List<Integer> listPos = listPosition.values().stream().toList();
        System.out.println("listPos: " + listPos.get(0));

        int index = 0;
        for (int i = 0; i < listPos.get(0); i++) {
            index += splitContent.get(i).length() + 1;
        }

        int beginIndex;
        int endIndex;

        beginIndex = Math.max(index - 135, 0);
        endIndex = Math.min(index + 135, content.length());
        snippet = "<... " + content.substring(beginIndex, endIndex) + " ...>";

        //log.info("index: {} beginIndex: {} endIndex: {}", index, beginIndex, endIndex);

        for (int i = 0; i < listPos.size(); i++) {
            String sourceWord = splitContent.get(listPos.get(i));
            index = 0;
            for (int j = 0; j < listPos.get(i); j++) {
                index += splitContent.get(j).length() + 1;
            }
            String repWord = " " + content.substring(index, index + sourceWord.length()) + " ";//splitContent.get(listPos.get(i));

            snippet = snippet.replaceAll(repWord.toLowerCase(Locale.ROOT), " <b>" + repWord + "</b> ");
        }
//        String repWord = splitContent.get(listPos.get(0));
//        snippet = "<... "+content.substring(beginIndex, endIndex).replace(repWord, " <b>" + repWord + "</b> ")+" ...>";

        //mapFoundWords.forEach((k, v) -> log.info("mapFoundWords : k: {} v:{}", k, v));
        //System.out.println();
        //listPosition.forEach((k, v) -> log.info("listPosition: k: {} v:{}", k, v));

        long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;
        System.out.println("timeElapsed: " + timeElapsed);
        System.out.println();

        return snippet;
    }

    private String getSnippet(Page page, List<Lemma> lemmaList) {
        log.info("Get snippet from pageId: {} ", page.getPageId());
        List<String> list = lemmaList.stream()
                .map(Lemma::getLemma).toList();
        String content = page.getContent();

        return getSnippet2(content, lemmaList);
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
