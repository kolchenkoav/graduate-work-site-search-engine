package searchengine.services;

public interface SearchService {
    Object search(String query, String site, int offset, int limit);
}
