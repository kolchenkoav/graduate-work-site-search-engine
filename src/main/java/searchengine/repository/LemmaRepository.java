package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    int countBySiteId(int siteId);

    boolean existsBySiteIdAndLemma(int siteId, String k);

    Lemma findBySiteIdAndLemma(int siteId, String k);

}
