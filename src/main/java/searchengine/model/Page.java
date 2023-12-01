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

@Table(name = "page", schema = "search_engine") //, indexes = @Index(columnList = "path")
public class Page  {                            // implements Serializable
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
    @Column(name = "code")
    private int code;

    @NonNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;


    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false)   //
    private SiteE siteE;

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL)
    private List<searchengine.model.Index> indices = new ArrayList<>();

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
                ", content='" + content + '\'' +
                ", siteE=" + siteE +
                ", indices=" + indices +
                '}';
    }
}
