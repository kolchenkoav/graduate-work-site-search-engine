package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.Objects;

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
    @Column(name = "page_id")
    private int pageId;

    @NonNull
    @Column(name = "lemma_id")
    private int lemmaId;

    @NonNull
    @Column(name = "rank_index", columnDefinition = "FLOAT")
    private double rank;

    @ManyToOne
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false)
    private Lemma lemma;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return indexId == index.indexId && pageId == index.pageId && lemmaId == index.lemmaId && Double.compare(index.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexId, pageId, lemmaId, rank);
    }
}
