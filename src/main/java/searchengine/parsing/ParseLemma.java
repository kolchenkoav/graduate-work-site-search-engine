package searchengine.parsing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import searchengine.lemma.LemmaFinder;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static searchengine.parsing.siteMapping.Utils.*;
import static searchengine.parsing.siteMapping.Utils.ANSI_RESET;

@Slf4j
@Component
@RequiredArgsConstructor
@Getter
@Setter
//@Transactional
//@Scope("prototype")
public class ParseLemma {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private int beginPos;
    private int endPos;
    private int currentPos;

    @Transactional
    public void parsing(Page page) {

        String content = page.getContent();
        int siteId = page.getSiteId();
        int pageId = page.getPageId();
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Map<String, Integer> mapLemmas = lemmaFinder.collectLemmas(content);
            Map<Lemma, Integer> mapLemmasForAdd = new HashMap<>();
            mapLemmas.forEach((key, value1) -> mapLemmasForAdd.put(parseOneLemma(siteId, key), value1));
            //mapLemmas.entrySet().parallelStream().parallel().forEach(lemma -> mapLemmasForAdd.put(parseOneLemma(siteId, lemma.getKey()), lemma.getValue()));

            lemmaRepository.saveAll(mapLemmasForAdd.keySet());
//            System.out.println();
//            mapLemmasForAdd.forEach((k, v) -> {
//                log.info("mapLemmasForAdd    Lemma: {} value: {}", k, v);
//            });

            List<IndexE> listIndexForAdd = new ArrayList<>();
            mapLemmasForAdd.forEach((key, value1) -> listIndexForAdd.add(new IndexE(pageId, key.getLemmaId(), value1)));
            //mapLemmasForAdd.entrySet().parallelStream().parallel().forEach(value -> listIndexForAdd.add(new IndexE(pageId, value.getKey().getLemmaId(), value.getValue())));
//            System.out.println();
//            listIndexForAdd.forEach((i) -> {
//                log.info("listIndexForAdd    IndexE: {}", i);
//            });
            indexRepository.saveAll(listIndexForAdd);

            printMessageAboutProgress(siteId, pageId, mapLemmasForAdd.size(), page.getPath());

        } catch (Exception e) {
            log.error("Ошибка parsing lemmas: {} siteId: {} pageId: {}", content.substring(0, 50) + "...", siteId, pageId);
        }
    }

    private void printMessageAboutProgress(int siteId, int pageId, int countOfLemmas, String url) {
        if ((endPos - beginPos) == 0 ) {
            log.info("Writing lemmas and indices: {} ", countOfLemmas);
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Writing lemmas and indices: ").append(ANSI_GREEN).append((currentPos - beginPos) * 100 / (endPos - beginPos)).append("% ");
        builder.append(ANSI_RESET).append(" siteId:").append(ANSI_CYAN).append(siteId).append(ANSI_RESET);
        builder.append(" pageId: ").append(ANSI_CYAN).append(pageId).append(ANSI_RESET);
        builder.append(" number of lemmas: ").append(ANSI_CYAN).append(countOfLemmas).append(ANSI_RESET);
        builder.append(" url: ").append(ANSI_BLUE).append(url).append(ANSI_RESET);
        System.out.print(builder + "\r");
    }

    private Lemma parseOneLemma(int siteId, String key) {
        Lemma result = lemmaRepository.findBySiteIdAndLemma(siteId, key).orElse(null);
        if (result == null) {
            result = new Lemma(siteId, key, 1);
        } else {
            result.setFrequency(result.getFrequency() + 1);
        }
        return result;
    }
}
