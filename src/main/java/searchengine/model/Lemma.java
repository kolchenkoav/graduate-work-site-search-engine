package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.Collection;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lemma", schema = "search_engine")
@SQLInsert(sql = "insert into search_engine.lemma(frequency, lemma, site_id ) values (?, ?, ?) on duplicate key update frequency = lemma.frequency + 1")
public class Lemma {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @NonNull
    @Column(name = "site_id", nullable = false)
    private int siteId;

    @NonNull
    @Column(name = "lemma", nullable = false)
    private String lemma;

    @NonNull
    @Column(name = "frequency", nullable = false)
    private int frequency;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "lemmaByLemmaId", cascade = CascadeType.MERGE)
    private Collection<Index> indexTSByLemmaId;

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "site_id", nullable = false, insertable = false, updatable = false)
    private Site siteBySiteId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lemma lemma = (Lemma) o;

        if (lemmaId != lemma.lemmaId) return false;
        if (siteId != lemma.siteId) return false;
        if (frequency != lemma.frequency) return false;
        return this.lemma.equals(lemma.lemma);
    }

    @Override
    public int hashCode() {
        int result = lemmaId;
        result = 31 * result + siteId;
        result = 31 * result + (lemma != null ? lemma.hashCode() : 0);
        result = 31 * result + frequency;
        return result;
    }
}
