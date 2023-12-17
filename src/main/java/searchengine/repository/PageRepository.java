package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;


@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Long>  {
    int countBySiteId(int siteId);

    void deleteBySiteId(int siteId);

    List<Page> findBySiteIdAndCode(int siteId, int code);

    void deleteAllBySiteId(int siteId);

    Page findBySiteIdAndPath(int siteId, String uri);

    Page findByPageId(Integer pageId);

    //List<Page> findAllById(List<Integer> list);

    //List<Page> findAllByPageId(List<Integer> list);
}
