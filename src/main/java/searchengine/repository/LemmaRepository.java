package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.Optional;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    int countBySiteId(int siteId);

    boolean existsBySiteIdAndLemma(int siteId, String k);

    Optional<Lemma> findBySiteIdAndLemma(int siteId, String k);

    Optional<Lemma> findByLemma(String k);
}
