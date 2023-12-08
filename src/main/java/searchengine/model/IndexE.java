package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "index_e", schema = "search_engine")
public class IndexE {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "index_id", nullable = false)
    private int indexId;

    @NonNull
    @Column(name = "page_id")
    private int pageId;

    @NonNull
    @Column(name = "lemma_id")
    private int lemmaId;

    @NonNull
    @Column(name = "rank_index", columnDefinition = "FLOAT")
    private double rank;

    @ManyToOne
    @JoinColumn(name = "page_id", insertable = false, updatable = false)  //, insertable = false, updatable = false
    private Page pageByPageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false) //, insertable = false, updatable = false
    private Lemma lemmaByLemmaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexE indexE = (IndexE) o;
        return indexId == indexE.indexId && pageId == indexE.pageId && lemmaId == indexE.lemmaId && Double.compare(indexE.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexId, pageId, lemmaId, rank);
    }

    @Override
    public String toString() {
        return "Index{" +
                "indexId=" + indexId +
                ", pageId=" + pageId +
                ", lemmaId=" + lemmaId +
                ", rank=" + rank +
                '}';
    }
}
