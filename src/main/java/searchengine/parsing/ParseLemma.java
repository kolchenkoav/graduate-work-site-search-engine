package searchengine.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.lemma.LemmaFinder;
import searchengine.lemma.LemmaFinderEn;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class ParseLemma {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public List<String> parsing(String text) {
        LemmaFinder lemmaFinder = null;
        LemmaFinderEn lemmaFinderEn = null;
        try {
            lemmaFinder = LemmaFinder.getInstance();
            lemmaFinderEn = LemmaFinderEn.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> listEn = lemmaFinderEn.getLemmaList(text);
        listEn.forEach(System.out::println);
        System.out.println();
        List<String> listRu = lemmaFinder.getLemmaList(text);
        listRu.forEach(System.out::println);
        System.out.println("============================");
        System.out.println();
        listEn.addAll(listRu);
        listEn.forEach(System.out::println);

        int frequency = 0;
        String lemmaText = "Хлеб";
        int siteId = 11;
        Lemma lemma = new Lemma(siteId, lemmaText, frequency);
        lemma = lemmaRepository.save(lemma);
        int lemmaId = lemma.getLemmaId();

        int pageId = 1130;
        int rank = 15;
        Index index = new Index(pageId, lemmaId, rank);
        indexRepository.save(index);

        return listEn;
    }
}
