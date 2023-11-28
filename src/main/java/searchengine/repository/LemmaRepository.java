package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteE;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    int countBySiteId(int siteId);
}
