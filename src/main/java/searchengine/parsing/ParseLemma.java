package searchengine.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.lemma.LemmaFinder;
import searchengine.lemma.LemmaFinderEn;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class ParseLemma {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    public List<String> parsing(String text) {
//        LemmaFinder lemmaFinder = null;
//        LemmaFinderEn lemmaFinderEn = null;
//        try {
//            lemmaFinder = LemmaFinder.getInstance();
//            lemmaFinderEn = LemmaFinderEn.getInstance();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        List<String> listEn = lemmaFinderEn.getLemmaList(text);
//        listEn.forEach(System.out::println);
//        System.out.println();
//        List<String> listRu = lemmaFinder.getLemmaList(text);
//        listRu.forEach(System.out::println);
//        System.out.println("============================");
//        System.out.println();
//        listEn.addAll(listRu);
//        listEn.forEach(System.out::println);

        SiteE siteE = new SiteE(Status.INDEXED, new Timestamp(System.currentTimeMillis()), "qqq", "Qu");
        siteE = siteRepository.save(siteE);
        int siteId = siteE.getSiteId();

        Page page = new Page(siteId, "path", 200, "context");
        page = pageRepository.save(page);
        int pageId = page.getPageId();;

        int frequency = 1;
        String lemmaText = "Хлеб";

        Lemma lemma = new Lemma(siteId, lemmaText, frequency);
        lemma = lemmaRepository.save(lemma);
        int lemmaId = lemma.getLemmaId();

        int rank = 15;
        Index index = new Index(pageId, lemmaId, rank);
        indexRepository.save(index);

        return null;
    }
}
