package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    Object startIndexing();
    Object stopIndexing();
    Object indexPage(String url);
}
