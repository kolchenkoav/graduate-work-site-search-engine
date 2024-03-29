package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteE;
import searchengine.model.Status;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<SiteE, Integer> {
    Optional<SiteE> findByName(String name);

    List<SiteE> findSiteEByName(String name);

    int countByNameAndStatus(String name, Status indexing);

    boolean existsByName(String name);

    SiteE getSiteEBySiteId(int siteId);
}
