package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    int countBySiteId(int siteId);

    boolean existsBySiteIdAndLemma(int siteId, String k);

    Optional<Lemma> findBySiteIdAndLemma(int siteId, String k);

    Optional<Lemma> findByLemma(String k);

    void deleteAllBySiteId(int siteId);

    List<Lemma> findAllByLemmaId(int lemmaId);

    void deleteBySiteIdAndFrequency(int siteId, int i);

    Lemma findByLemmaId(int lemmaId);

    boolean existsBySiteId(int siteId);
}
