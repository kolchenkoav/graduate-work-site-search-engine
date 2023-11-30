package searchengine.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.lemma.LemmaFinder;
import searchengine.lemma.LemmaFinderEn;

import java.io.IOException;
import java.util.List;


@Slf4j
@Component
public class ParseLemma {

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

        return listEn;
    }
}
