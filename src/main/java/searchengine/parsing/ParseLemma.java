package searchengine.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

    public void parsing(String text, int siteId, int pageId) {
        int frequency = 0;
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        assert lemmaFinder != null;
        Map<String, Integer> mapLemmas = lemmaFinder.collectLemmas(text);

        for (Map.Entry<String, Integer> lemmaE: mapLemmas.entrySet()) {
            // on duplicate key update frequency = lemma.frequency + 1
//            if (siteId > 1) {
//                System.out.println("===========>>>> "+siteId);
//            }
            Lemma lemma;
            if (lemmaRepository.existsBySiteIdAndLemma(siteId, lemmaE.getKey())) {
                lemma = lemmaRepository.findBySiteIdAndLemma(siteId, lemmaE.getKey());
                frequency = lemma.getFrequency() + 1;
            } else {
                lemma = new Lemma(siteId, lemmaE.getKey(), 1);
            }

            lemma = lemmaRepository.save(lemma);
            int lemmaId = lemma.getLemmaId();

            // index ===============
            Index index = new Index(pageId, lemmaId, lemmaE.getValue());
            indexRepository.save(index);
        }
    }
}
