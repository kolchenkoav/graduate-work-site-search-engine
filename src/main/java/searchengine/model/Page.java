package searchengine.model;

import lombok.*;

import javax.persistence.Index;
import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
//, indexes = @Index(name = "fn_index", columnList = "path")
@Table(name = "page", schema = "search_engine")
public class Page {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "page_id")
    private int pageId;

    @NonNull
    @Column(name = "site_id")
    private int siteId;

    @NonNull
    @Column(columnDefinition = "TEXT")
    private String path;

    @NonNull
    @Column(name = "code", nullable = false)
    private int code;

    @NonNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

//    @NonNull
//    @Column(name = "title", length = 255)
//    private String title;

//    @OnDelete(action = OnDeleteAction.CASCADE)
//    @OneToMany(mappedBy = "pageByPageId", cascade = CascadeType.MERGE)
//    private Collection<> indexTSByPageId;

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "site_id", nullable = false, insertable = false, updatable = false)
    private Site siteBySiteId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return pageId == page.pageId && siteId == page.siteId && code == page.code
                && path.equals(page.path) && content.equals(page.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, siteId, path, code, content);
    }
}
