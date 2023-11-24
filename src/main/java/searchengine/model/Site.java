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
public class Site {
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

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "siteBySiteId", cascade = CascadeType.MERGE)
    private Collection<Lemma> lemmaSBySiteId;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "siteBySiteId", cascade = CascadeType.MERGE)
    private Collection<Page> pageBySiteId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return siteId == site.siteId && status == site.status
                && statusTime.equals(site.statusTime) && url.equals(site.url) && name.equals(site.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, status, statusTime, url, name);
    }
}