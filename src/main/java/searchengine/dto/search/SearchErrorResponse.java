package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchErrorResponse {
    private boolean result;
    private String error;
}
