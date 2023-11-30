package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lemma", schema = "search_engine")
@SQLInsert(sql = "insert into search_engine.lemma(frequency, lemma, site_id ) values (?, ?, ?) on duplicate key update frequency = lemma.frequency + 1")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @NonNull
    @Column(name = "site_id")
    private int siteId;

    @NonNull
    @Column(name = "lemma")
    private String lemma;

    @NonNull
    @Column(name = "frequency")
    private int frequency;

    @OneToMany(mappedBy = "lemmaId", cascade = CascadeType.ALL)
    private List<Index> indices = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false) //, insertable = false, updatable = false
    private SiteE siteE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return lemmaId == lemma1.lemmaId && siteId == lemma1.siteId && frequency == lemma1.frequency && lemma.equals(lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemmaId, siteId, lemma, frequency);
    }
}
