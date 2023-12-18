package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Getter
@Setter
@ToString
public class SearchResults {
    private int number;
    private int siteId;
    private int pageId;
    private String url;
    private String title;
    private String snippet;
    private double relevance;
}
