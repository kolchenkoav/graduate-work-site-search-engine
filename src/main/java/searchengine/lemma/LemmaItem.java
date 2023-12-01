package searchengine.lemma;

import lombok.Data;

@Data
public class LemmaItem {
    private final String lemma;
    private final String originWord;
}