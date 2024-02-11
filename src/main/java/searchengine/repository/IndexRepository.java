package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexE;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<IndexE, Long> {
    Optional<List<IndexE>> findByLemmaId(int lemmaId);

    List<IndexE> findByPageId(int pageId);

    Optional<IndexE> findByLemmaIdAndPageId(int lemmaId, int pageId);
}
