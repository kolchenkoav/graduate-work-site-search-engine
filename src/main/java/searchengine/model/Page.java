package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "page", schema = "search_engine", indexes = @Index(columnList = "path"))
public class Page  {                            // implements Serializable
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "page_id")
    private int pageId;

    @NonNull
    @Column(name = "site_id")
    private int siteId;

    @NonNull
    @Basic(optional = false)
    @Column(name = "path", length =255)
    private String path;

    @NonNull
    @Column(name = "code")
    private int code;

    @NonNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @NonNull
    @Column(name = "title", length = 255)
    private String title;

    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false)   //
    private SiteE siteEBySiteId;

    @OneToMany(mappedBy = "pageByPageId", cascade = CascadeType.ALL)
    private List<IndexE> IndexEByPageId = new ArrayList<>();

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

    @Override
    public String toString() {
        return "Page{" +
                "pageId=" + pageId +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content(trim by 100)='" + content.substring(0, 100) + '\'' +
                '}';
    }
}
