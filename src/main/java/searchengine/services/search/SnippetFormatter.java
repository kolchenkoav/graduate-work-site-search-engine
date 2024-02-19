package searchengine.services.search;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Lemma;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Component
public class SnippetFormatter {
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    /**
     * Получение сниппета из контекста страницы
     *
     * @param content   для поиска слов и вырезания фрагмента
     * @param lemmaList список лемм для поиска
     * @return сниппет
     */
    public String getSnippet(String content, List<Lemma> lemmaList) {
        long startTime = System.currentTimeMillis();

        List<String> splitContent = Arrays.stream(content.trim().split("\\s+")).toList();
        String[] arraySplitContent = splitContent.toArray(String[]::new);

        Map<String, List<Integer>> listPosition = new HashMap<>();
        for (Lemma lemma : lemmaList) {
            List<Integer> listIndexByLemmaFromContent = getListIndexByLemmaFromContent(
                    arraySplitContent, lemma.getLemma());
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
            snippet = getSnippet(mapFoundWords, splitContent, content);
        } catch (Exception e) {
            log.warn("debug: snippet is ''");
        }

        log.debug("timeElapsed: {}", System.currentTimeMillis() - startTime);
        return snippet;
    }

    private boolean isFoundIndexNear(int prevPosition, int cur) {
        return prevPosition == cur || prevPosition == cur - 1 || prevPosition == cur - 2
                || prevPosition == cur - 3 ||
                prevPosition == cur + 1 || prevPosition == cur + 2 || prevPosition == cur + 3;
    }

    /**
     * Возвращает список индексов для искомого слова
     *
     * @param arraySplitContent массив слов контекста
     * @param wordLemma         слово
     * @return индексы
     */
    private List<Integer> getListIndexByLemmaFromContent(String[] arraySplitContent,
                                                         @NonNull String wordLemma) {
        return IntStream.range(0, arraySplitContent.length)
                .mapToObj(index -> {
                    String w = arraySplitContent[index];
                    if (w.endsWith("'s")) {
                        w = w.replace("'s", "");
                    }
                    if (w.endsWith(".com")) {
                        w = w.replace(".com", "");
                    }
                    List<String> lemmaListFromQuery = Objects.requireNonNull(lemmaFinder)
                            .collectLemmas(w)
                            .keySet()
                            .stream()
                            .toList();
                    String result = "";
                    if (!lemmaListFromQuery.isEmpty()) {
                        String regex = "[^A-Za-zА-Яа-я0-9]";
                        result = lemmaListFromQuery.get(0).replaceAll(regex, " ").trim()
                                .toLowerCase(Locale.ROOT);
                    }
                    return String.format("%d -> %s", index, result);
                })
                .filter(s -> s.toLowerCase(Locale.ROOT).endsWith(wordLemma.toLowerCase(Locale.ROOT)))
                .map(s -> s.substring(0, s.indexOf("->")).trim())
                .parallel()
                .mapToInt(Integer::parseInt).boxed().toList();
    }


    /**
     * Получение mapFoundWords для правильного отображения списка лемм
     *
     * @param listPosition список   < лемма, список индексов в контесте для этой леммы>
     * @param lemmaList    поисковые леммы
     * @return mapFoundWords    < лемма, индекс >
     */
    private Map<String, Integer> setMapFoundWords(Map<String, List<Integer>> listPosition,
                                                  List<Lemma> lemmaList) {
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

    /**
     * Находит нужный фрагмент и выделяет слова жирным
     *
     * @param mapFoundWords мапа слово - позиция
     * @param splitContent  - список слов контента
     * @param content       контент
     * @return сниппет
     */
    private String getSnippet(Map<String, Integer> mapFoundWords,
                              List<String> splitContent, String content) {

        String snippet = "";
        List<Integer> listPos = mapFoundWords.values().stream().toList();

        int index = 0;
        for (int i = 0; i < listPos.get(0); i++) {
            index += splitContent.get(i).length() + 1;
        }

        int beginIndex;
        int endIndex;

        int SNIPPET_OFFSET = 130;
        beginIndex = Math.max(index - SNIPPET_OFFSET, 0);                       // начало сниппета
        endIndex = Math.min(index + SNIPPET_OFFSET, content.length());          // конец сниппета
        snippet = "<... " + content.substring(beginIndex, endIndex) + " ...>";  // обрамление

        /* Выделение слов ЖИРНЫМ */
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
}
