package searchengine.lemma;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmaFinder {

    private final LuceneMorphology morphologyRus;
    private final LuceneMorphology morphologyEng;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "CONJ",
        "INT", "PREP", "ARTICLE", "PART"};

    public static LemmaFinder getInstance() {
        try {
            LuceneMorphology morphologyRus = new RussianLuceneMorphology();
            LuceneMorphology morphologyEng = new EnglishLuceneMorphology();
            return new LemmaFinder(morphologyRus, morphologyEng);
        } catch (IOException e) {//
        }
        return null;
    }

    private LemmaFinder(LuceneMorphology luceneMorphologyRus,
        LuceneMorphology luceneMorphologyEng) {
        this.morphologyRus = luceneMorphologyRus;
        this.morphologyEng = luceneMorphologyEng;
    }

    private LemmaFinder() {
        throw new IllegalArgumentException("Disallow construct");
    }

    /**
     * Метод разделяет текст на слова, находит все леммы и считает их количество.
     *
     * @param text текст из которого будут выбираться леммы
     * @return ключ является леммой, а значение количеством найденных лемм
     */
    public Map<String, Integer> collectLemmas(String text) {
        String[] words = splitWords(text, false);

        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            String normalWord = getNormalWord(word);
            if (normalWord == null) {
                continue;
            }
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }
        return lemmas;
    }

    public Map<LemmaItem, Integer> collectLemmasMap(String text) {
        String[] words = splitWords(text, false);

        HashMap<LemmaItem, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            String normalWord = getNormalWord(word);
            if (normalWord == null) {
                continue;
            }
            lemmas.put(new LemmaItem(normalWord, word),
                lemmas.getOrDefault(new LemmaItem(normalWord, word), 0) + 1);
        }
        return lemmas;
    }

    /**
     * Метод разделяет текст на слова, находит все леммы и считает их количество.
     *
     * @param text текст из которого будут выбираться леммы
     * @return ключ является леммой, а значение количеством найденных лемм
     */
    public Map<Integer, String> collectLemmasList(String text) {
        String[] words = splitWords(text, true);

        HashMap<Integer, String> lemmas = new HashMap<>();
        int i = 0;
        for (String word : words) {
            i++;
            String normalWord = getNormalWord(word);
            if (normalWord == null) {
                continue;
            }
            lemmas.put(i, normalWord);
        }
        return lemmas;
    }

    /**
     * Метод разделяет текст на слова
     *
     * @param text текст из которого будут выбираться леммы
     * @return лист лемм
     */
    public List<String> getLemmaList(String text) {
        String[] words = splitWords(text, true);

        List<String> lemmas = new ArrayList<>();
        for (String word : words) {
            String normalWord = getNormalWord(word);
            if (normalWord == null) {
                continue;
            }
            lemmas.add(normalWord);
        }
        return lemmas;
    }

    /**
     * Метод возвращает лемму
     *
     * @param word слово
     * @return лемма
     */
    public String getLemma(String word) {
        return getNormalWord(word);
    }

    private String getNormalWord(String word) {
        LuceneMorphology luceneMorphology;
        if (word.isBlank()) {
            return null;
        }

        if (isRussian(word)) {
            luceneMorphology = morphologyRus;
        } else {
            luceneMorphology = morphologyEng;
        }
        try {
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        List<String> normalForms = luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return null;
        }
        return normalForms.get(0);
    }

    private String[] splitWords(String text, boolean saveOriginal) {
        String[] split;
        if (saveOriginal) {
            split = text.toLowerCase(Locale.ROOT)
                .trim()
                .split("\\s+");

        } else {
            split = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ")
                .trim()
                .split("\\s+");
        }
        return split;
    }

    private static boolean isRussian(String word) {
        return word.chars()
            .mapToObj(Character.UnicodeBlock::of)
            .anyMatch(Character.UnicodeBlock.CYRILLIC::equals);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
}
