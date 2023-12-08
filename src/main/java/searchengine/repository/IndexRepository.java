package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexE;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<IndexE, Long> {
    Optional<List<IndexE>> findByLemmaId(int lemmaId);

}
