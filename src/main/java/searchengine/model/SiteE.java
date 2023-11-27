package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "site", schema = "search_engine")
public class SiteE {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_id", nullable = false)
    private int siteId;

    @NonNull
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;

    @NonNull
    @Column(name = "status_time")
    private Timestamp statusTime;

    @Basic
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @NonNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;

    @NonNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(cascade = CascadeType.MERGE)
    private Collection<Lemma> lemmaBySiteEId;

    @OneToMany(cascade = CascadeType.MERGE)
    private Collection<Page> pageBySiteEId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteE siteE = (SiteE) o;
        return siteId == siteE.siteId && status == siteE.status
                && statusTime.equals(siteE.statusTime) && url.equals(siteE.url) && name.equals(siteE.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, status, statusTime, url, name);
    }
}