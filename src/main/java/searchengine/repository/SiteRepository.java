package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteE;

@Repository
public interface SiteRepository extends JpaRepository<SiteE, Long> {
}
