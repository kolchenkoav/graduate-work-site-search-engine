package searchengine.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.Response;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.lemma.LemmaFinder;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final SiteList sites;
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    private double[][] relevance;
    private double maxRelevance;
    private List<SearchResults> searchResultsList;
    int offset;
    int limit;

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
    @Override
    public Response search(String query, String site, int offset, int limit) {
        printInfoBySearch(query, site, offset, limit);
        this.offset = offset;   // Необходим в методе formationForOneSite и sortSearchResultsList()
        this.limit = limit;     // - // -

        List<Integer> siteIdList = getSiteIdList(site);
        if (siteIdList.isEmpty()) {
            return setResponseFalse("Search site " + site + " not found");
        }

        List<String> lemmaListFromQuery = Objects.requireNonNull(lemmaFinder).collectLemmas(query)
                .keySet()
                .stream()
                .toList();

        List<Lemma> lemmaList = getLemmaList(siteIdList, lemmaListFromQuery);
        if (lemmaList.isEmpty()) {
            return setResponseFalse("search lemmas: not found in database");
        }

        removeIfLimitFrequencyIsBig(lemmaList);
        if (lemmaList.isEmpty()) {
            return setResponseFalse("Not found lemmas in DB");
        }

        siteIdList = lemmaList.stream().map(Lemma::getSiteId).distinct().toList();

        lemmaList = lemmaList.stream().sorted(Comparator.comparingInt(Lemma::getFrequency)).toList();

        fillSearchResultsList(siteIdList, lemmaList);
        if (searchResultsList.isEmpty()) {
            return setResponseFalse("");
        }
        sortSearchResultsList();

        setSnippetForSearchResults(lemmaList);

        return setSearchData();
    }

    private void printInfoBySearch(String query, String site, int offset, int limit) {
        System.out.println();
        log.info("=========================================");
        log.info("Поисковый запрос: {}", query);
        log.info("Сайт: {}", site);
        log.info("Сдвиг от 0: {}", offset);
        log.info("Количество результатов: {}", limit);
        log.info("=========================================");
    }

    /**
     * Возвращает список сущностей Lemma из DB если количество лемм совпадает
     *
     * @param siteIdList список siteId
     * @param lemmaListFromQuery список лемм из запроса
     * @return список найденных в БД лемм
     */
    private List<Lemma> getLemmaList(List<Integer> siteIdList, List<String> lemmaListFromQuery) {
        List<Lemma> lemmaList = new ArrayList<>();
        for (Integer siteId : siteIdList) {
            for (String lem : lemmaListFromQuery) {
                lemmaRepository.findBySiteIdAndLemma(siteId, lem).ifPresent(lemmaList::add);
            }
            long countOfWordsFound = lemmaList.stream().filter(lemma -> lemma.getSiteId() == siteId).count();
            if (countOfWordsFound == 0) {
                continue;
            }
            if (countOfWordsFound != lemmaListFromQuery.size()) {
                lemmaList.removeIf(lemma -> lemma.getSiteId() == siteId);
            }
        }
        lemmaList.removeIf(lemma -> lemma.getLemma().length() == 1);
        return lemmaList;
    }

    /**
     * Удаление из списка поиска лемм которые слишком часто встречаются
     *
     * @param lemmaList список лемм
     */
    private void removeIfLimitFrequencyIsBig(List<Lemma> lemmaList) {
        int limitCount = 10;
        Iterator<Lemma> iterator = lemmaList.iterator();
        while (iterator.hasNext()) {
            Lemma lemma = iterator.next();
            int countPages = pageRepository.countBySiteId(lemma.getSiteId());

            log.info("siteId: {} countPages: {} Frequency: {}", lemma.getSiteId(), countPages, lemma.getFrequency());
            if (lemma.getFrequency() >= countPages && countPages > limitCount) {
                iterator.remove();
            }
        }
    }

    /**
     * Заполнение списка SearchResults
     *
     * @param siteIdList список siteId
     * @param lemmaList список лемм
     */
    private void fillSearchResultsList(List<Integer> siteIdList, List<Lemma> lemmaList) {
        searchResultsList = new ArrayList<>();
        for (Integer i : siteIdList) {
            formationForOneSite(lemmaList.stream().filter(lemma -> lemma.getSiteId() == i).toList());

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
    }

    /**
     *  Сортировка списка SearchResults и после применение offset и limit
     */
    private void sortSearchResultsList() {
        searchResultsList = searchResultsList.stream()
                .sorted(Comparator
                        .comparing(SearchResults::getRelevance)
                        .reversed())
                .skip(offset)
                .limit(limit)
                .toList();
    }

    /**
     * Заполнение сниппетами списка SearchResults
     *
     * @param lemmaList список лемм
     */
    private void setSnippetForSearchResults(List<Lemma> lemmaList) {
        Iterator<SearchResults> iteratorSR = searchResultsList.iterator();
        SearchResults results;
        while (iteratorSR.hasNext()) {
            results = iteratorSR.next();
            Page page = pageRepository.findByPageId(results.getPageId());
            results.setTitle(page.getTitle());
            results.setUrl(page.getPath());

            String snippet = getSnippet(page.getContent(), lemmaList);
            results.setSnippet(snippet);
        }
    }

    /**
     * SearchResults -> searchDataList
     *
     * @return responseTrue
     */
    private Response setSearchData() {
        List<SearchData> searchDataList = new ArrayList<>();
        SearchResponse responseTrue = new SearchResponse();
        responseTrue.setError("");
        responseTrue.setResult(true);
        responseTrue.setCount(searchResultsList.size());
        for (SearchResults searchResults : searchResultsList) {
            SiteE siteE = siteRepository.getSiteEBySiteId(searchResults.getSiteId());
            String url = searchResults.getUrl(); // siteE.getUrl() +
            SearchData searchData = new SearchData(siteE.getName(),
                    siteE.getName(),
                    url,
                    searchResults.getTitle(),
                    searchResults.getSnippet(),
                    searchResults.getRelevance());
            searchDataList.add(searchData);
        }
        responseTrue.setData(searchDataList);
        return responseTrue;
    }

    /**
     * Получает список Id сайтов из конфигурации которые есть в БД
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
    private void formationForOneSite(List<Lemma> lemmaList) {
        // 4. По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается
        List<IndexE> indexList = new ArrayList<>(Objects
                .requireNonNull(indexRepository.findByLemmaId(lemmaList.get(0).getLemmaId())
                        .orElse(null)).stream().skip(offset).limit(limit).toList() );
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
                    relevance[j][k] = j + 1.0;
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
     * Получение сниппета из контекста страницы
     *
     * @param content   для поиска слов и вырезания фрагмента
     * @param lemmaList список лемм для поиска
     * @return сниппет
     */
    private String getSnippet(String content, List<Lemma> lemmaList) {
        long startTime = System.currentTimeMillis();

        List<String> splitContent = Arrays.stream(content.trim().split("\\s+")).toList();
        String[] arraySplitContent = splitContent.toArray(String[]::new);

        Map<String, List<Integer>> listPosition = new HashMap<>();
        for (Lemma lemma : lemmaList) {
            List<Integer> listIndexByLemmaFromContent = getListIndexByLemmaFromContent(arraySplitContent, lemma.getLemma());
            listPosition.put(lemma.getLemma(), listIndexByLemmaFromContent);
        }

        Map<String, Integer> mapFoundWords = new HashMap<>();
        try {
            mapFoundWords = setMapFoundWords(listPosition, lemmaList);
        } catch (Exception e) {
            log.warn("debug: mapFoundWords is null");
        }


        String snippet = "";
        try {
            snippet = findSnippet(mapFoundWords, splitContent, content);
        } catch (Exception e) {
            log.warn("debug: snippet is ''");
        }

        log.info("timeElapsed: {}", System.currentTimeMillis() - startTime);
        return snippet;
    }

    /**
     * Получение mapFoundWords для правильного отображения списка лемм
     *
     * @param listPosition список   < лемма, список индексов в контесте для этой леммы>
     * @param lemmaList поисковые леммы
     * @return mapFoundWords    < лемма, индекс >
     */
    private Map<String, Integer> setMapFoundWords(Map<String, List<Integer>> listPosition, List<Lemma> lemmaList) {
        Map<String, Integer> mapFoundWords = new HashMap<>();
        for (Lemma lemma : lemmaList) {
            mapFoundWords.put(lemma.getLemma(), 0);
        }

        AtomicInteger prev = new AtomicInteger(0);
        listPosition.forEach((k, v) -> {
            if (!v.isEmpty()) {
                mapFoundWords.put(k, v.get(0));
            } else {
                mapFoundWords.put(k, 1);
            }
            if (v.size() == 1) {
                prev.set(v.get(0));
            }
        });
        if (prev.get() == 0) {
            prev.set(listPosition.get(lemmaList.get(0).getLemma()).get(0));
        }

        mapFoundWords.forEach((k, v) -> {
            int cur = v;
            int prevPosition = prev.get();
            if (isFoundIndexNear(prevPosition, cur)) {
                prev.set(cur);
            } else {
                listPosition.get(k).forEach(val -> {
                    int cur2 = val;
                    int prevPosition2 = prev.get();
                    if (isFoundIndexNear(prevPosition2, cur2)) {
                        prev.set(cur2);
                        mapFoundWords.put(k, val);
                    }
                });
            }
        });
        return mapFoundWords;
    }

    private boolean isFoundIndexNear(int prevPosition, int cur) {
        return prevPosition == cur || prevPosition == cur - 1 || prevPosition == cur - 2 || prevPosition == cur - 3 ||
               prevPosition == cur + 1 || prevPosition == cur + 2 || prevPosition == cur + 3;
    }

    /**
     * Возвращает список индексов для искомого слова
     *
     * @param arraySplitContent массив слов контекста
     * @param wordLemma         слово
     * @return индексы
     */
    private List<Integer> getListIndexByLemmaFromContent(String[] arraySplitContent, @NonNull String wordLemma) {
        return IntStream.range(0, arraySplitContent.length)
                .mapToObj(index -> {
                    String w = arraySplitContent[index];
                    if (w.endsWith("'s")) {
                        w = w.replace("'s", "");
                    }
                    if (w.endsWith(".com")) {
                        w = w.replace(".com", "");
                    }
                    List<String> lemmaListFromQuery = Objects.requireNonNull(lemmaFinder).collectLemmas(w)
                                    .keySet()
                                    .stream()
                                    .toList();
                    String result = "";
                    if (!lemmaListFromQuery.isEmpty()) {
                        String regex = "[^A-Za-zА-Яа-я0-9]";
                        result = lemmaListFromQuery.get(0).replaceAll(regex, " ").trim().toLowerCase(Locale.ROOT);
                    }
                    return String.format("%d -> %s", index, result);
                })
                .filter(s -> s.toLowerCase(Locale.ROOT).endsWith(wordLemma.toLowerCase(Locale.ROOT)))
                .map(s -> s.substring(0, s.indexOf("->")).trim())
                .parallel()
                .mapToInt(Integer::parseInt).boxed().toList();
    }

    /**
     * Находит нужный фрагмент и выделяет слова жирным
     *
     * @param mapFoundWords мапа слово - позиция
     * @param splitContent
     * @param content      контент
     * @return сниппет
     */
    private String findSnippet(Map<String, Integer> mapFoundWords, List<String> splitContent, String content) {
        String snippet = "";
        List<Integer> listPos = mapFoundWords.values().stream().toList();

        int index = 0;
        for (int i = 0; i < listPos.get(0); i++) {
            index += splitContent.get(i).length() + 1;
        }

        int beginIndex;
        int endIndex;

        beginIndex = Math.max(index - 130, 0);
        endIndex = Math.min(index + 130, content.length());
        snippet = "<... " + content.substring(beginIndex, endIndex) + " ...>";

        for (Integer position : listPos) {
            String sourceWord = splitContent.get(position);
            index = 0;
            for (int j = 0; j < position; j++) {
                index += splitContent.get(j).length() + 1;
            }
            String repWord = " " + content.substring(index, index + sourceWord.length()) + " ";

            try {
                snippet = snippet.replaceAll(repWord, " <b>" + repWord.trim() + "</b> ");
            } catch (Exception e) {
                log.warn("repWord: {} snippet: {}", repWord, snippet);
            }
        }
        return snippet;
    }

    /**
     * response.setResult(true) true- тогда удаляются результаты предыдущего поиска,
     * но не отражается строка ошибки на стороне фронта
     * если false- то оставляет результаты предыдущего поиска
     *
     * @param errorMessage сообщение
     * @return response ответ
     */
    private Response setResponseFalse(String errorMessage) {
        log.warn(errorMessage);

        List<SearchData> searchDataList = new ArrayList<>();
        SearchResponse response = new SearchResponse();
        response.setError(errorMessage);
        response.setResult(true);
        response.setCount(0);
        SearchData searchData = new SearchData("", "", "", "", "", 0);
        searchDataList.add(searchData);
        response.setData(searchDataList);

        return response;
    }
}
