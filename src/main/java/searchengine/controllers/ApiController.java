package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteE;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.sql.Timestamp;
import java.time.Instant;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final SiteRepository siteRepository;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        statistics();
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String nameUrl) {
        return ResponseEntity.ok(indexingService.indexPage(nameUrl));
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam String query,
                                         @RequestParam(required = false) String site,
                                         @RequestParam int offset,
                                         @RequestParam(required = false) int limit) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        SiteE siteE = new SiteE();
        siteE.setName("PlayBack.Ru");
        siteE.setUrl("https://www.playback.ru");
        siteE.setLastError("not error");
        siteE.setStatusTime(Timestamp.from(Instant.now()));
        siteE.setStatus(Status.INDEXED);
        siteRepository.save(siteE);
        return ResponseEntity.ok("");
    }
}
