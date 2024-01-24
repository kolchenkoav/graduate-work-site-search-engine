package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Long> {
    int countBySiteId(int siteId);

    List<Page> findBySiteIdAndCode(int siteId, int code);

    void deleteAllBySiteId(int siteId);

    Page findBySiteIdAndPath(int siteId, String uri);

    Page findByPageId(Integer pageId);
}
