package searchengine.dto.indexing;

import lombok.Data;
import searchengine.dto.Response;

@Data
public class IndexingErrorResponse extends Response {
    private String error;
}
