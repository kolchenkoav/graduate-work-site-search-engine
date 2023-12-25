package searchengine.dto.search;

import lombok.Data;
import searchengine.dto.Response;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResponse extends Response {
    private int count;
    private String error;
    private List<SearchData> data = new ArrayList<>();
}
