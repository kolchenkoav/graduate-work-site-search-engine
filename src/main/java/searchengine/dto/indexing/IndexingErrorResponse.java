package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingErrorResponse {
    private boolean result;
    private String error;
}
