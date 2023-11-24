package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "index_s", schema = "search_engine")
public class Index {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "index_id", nullable = false)
    private int indexId;

    @NonNull
    @Column(name = "page_id", nullable = false)
    private int pageId;

    @NonNull
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @NonNull
    @Column(name = "rank_index", columnDefinition = "FLOAT")
    private double rank;

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "page_id", nullable = false, insertable = false, updatable = false)
    private Page pageByPageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "lemma_id", nullable = false, insertable = false, updatable = false)
    private Lemma lemmaByLemmaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Index index = (Index) o;

        if (indexId != index.indexId) return false;
        if (pageId != index.pageId) return false;
        if (lemmaId != index.lemmaId) return false;
        return Double.compare(index.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = indexId;
        result = 31 * result + pageId;
        result = 31 * result + lemmaId;
        temp = Double.doubleToLongBits(rank);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
