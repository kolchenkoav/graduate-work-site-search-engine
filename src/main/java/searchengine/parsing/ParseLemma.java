package searchengine.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.aop.Loggable;
import searchengine.lemma.LemmaFinder;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParseLemma {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Loggable
    public void parsing(String text, int siteId, int pageId) {
        int frequency = 0;
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        if (lemmaFinder == null) {
            log.warn("lemmaFinder is null");
            return;
        }

        Map<String, Integer> mapLemmas = lemmaFinder.collectLemmas(text);
        System.out.println("mapLemmas.size(): " + mapLemmas.size());

        for (Map.Entry<String, Integer> lemmaValue : mapLemmas.entrySet()) {
            Lemma lemma = null;
            try {
                lemma = lemmaRepository.findBySiteIdAndLemma(siteId, lemmaValue.getKey()).orElse(null);
            } catch (Exception e) {
                log.warn("lemma = null");
            }
            if (lemma != null) {
                frequency = lemma.getFrequency() + 1;
                lemma.setFrequency(frequency);
            } else {
                lemma = new Lemma(siteId, lemmaValue.getKey(), 1);
            }

            lemma = lemmaRepository.save(lemma);
            int lemmaId = lemma.getLemmaId();

            // index ===============
            IndexE indexE = new IndexE(pageId, lemmaId, lemmaValue.getValue());
            indexRepository.save(indexE);
        }
    }
}
